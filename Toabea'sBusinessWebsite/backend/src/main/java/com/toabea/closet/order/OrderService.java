package com.toabea.closet.order;
import com.toabea.closet.common.*;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.*;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.Instant;
import java.util.*;

@Service
public class OrderService {
  private final OrderRepository orders;
  private final ProductRepository products;
  private final CustomerRepository customers;
  private final InventoryTransactionRepository tx;
  private final MongoTemplate mongo;
  private final BigDecimal feePercent;
  private final BigDecimal feeFlat;

  public OrderService(OrderRepository o, ProductRepository p, CustomerRepository c, InventoryTransactionRepository t, MongoTemplate m,
      @Value("${app.paystack.fee-percent:1.95}") BigDecimal feePercent, @Value("${app.paystack.fee-flat:0}") BigDecimal feeFlat) {
    orders = o; products = p; customers = c; tx = t; mongo = m; this.feePercent = feePercent; this.feeFlat = feeFlat;
  }

  @Transactional
  public Order create(CreateOrderRequest r) {
    String method = "PAYSTACK".equalsIgnoreCase(r.paymentMethod()) ? "PAYSTACK" : "CASH";
    Customer c = customers.findByEmailIgnoreCase(r.email()).orElseGet(Customer::new);
    c.setName(r.customerName()); c.setEmail(r.email()); c.setPhone(r.phone()); c.setAddress(r.address()); c.setUpdatedAt(Instant.now());
    c = customers.save(c);

    Order o = new Order();
    o.setId(new org.bson.types.ObjectId().toString());
    o.setOrderNumber("TB-" + System.currentTimeMillis());
    o.setCustomerId(c.getId()); o.setCustomerName(r.customerName()); o.setEmail(r.email()); o.setPhone(r.phone()); o.setAddress(r.address());
    o.setPaymentMethod(method);

    BigDecimal subtotal = BigDecimal.ZERO;
    for (Line l : r.items()) {
      Product p = products.findById(l.productId()).orElseThrow(() -> new IllegalArgumentException("Product not found: " + l.productId()));
      if (l.quantity() < 1) throw new IllegalArgumentException("Quantity must be positive");
      boolean hasSizes = p.getSizes() != null && !p.getSizes().isEmpty();
      String size = l.size() != null ? l.size().trim() : null;
      if (hasSizes && (size == null || size.isBlank())) throw new IllegalArgumentException("Select a size for " + p.getName());

      Product updated;
      if (hasSizes) {
        Query q = new Query(Criteria.where("_id").is(p.getId()).and("active").is(true)
            .and("sizes").elemMatch(Criteria.where("size").is(size).and("stock").gte(l.quantity())));
        Update u = new Update().inc("stock", -l.quantity()).inc("sizes.$[elem].stock", -l.quantity())
            .filterArray(Criteria.where("elem.size").is(size)).set("updatedAt", Instant.now());
        updated = mongo.findAndModify(q, u, FindAndModifyOptions.options().returnNew(true), Product.class);
        if (updated == null) throw new IllegalStateException("Insufficient stock for " + p.getName() + " (" + size + ")");
      } else {
        Query q = new Query(Criteria.where("_id").is(p.getId()).and("stock").gte(l.quantity()).and("active").is(true));
        Update u = new Update().inc("stock", -l.quantity()).set("updatedAt", Instant.now());
        updated = mongo.findAndModify(q, u, FindAndModifyOptions.options().returnNew(true), Product.class);
        if (updated == null) throw new IllegalStateException("Insufficient stock for " + p.getName());
      }

      OrderItem i = new OrderItem();
      i.setProductId(p.getId()); i.setName(p.getName()); i.setSize(size);
      i.setUnitPrice(p.getPrice()); i.setQuantity(l.quantity()); i.setLineTotal(p.getPrice().multiply(BigDecimal.valueOf(l.quantity())));
      o.getItems().add(i);
      subtotal = subtotal.add(i.getLineTotal());

      int balanceAfter = hasSizes
          ? updated.getSizes().stream().filter(sz -> sz.getSize().equals(size)).findFirst().map(Product.ProductSize::getStock).orElse(updated.getStock())
          : updated.getStock();
      InventoryTransaction it = new InventoryTransaction();
      it.setProductId(p.getId()); it.setOrderId(o.getId()); it.setOrderNumber(o.getOrderNumber()); it.setCustomerName(r.customerName()); it.setSize(size);
      it.setQuantityDelta(-l.quantity()); it.setBalanceAfter(balanceAfter); it.setReason("ORDER");
      tx.save(it);
    }

    BigDecimal fee = method.equals("PAYSTACK") ? calculateFee(subtotal) : BigDecimal.ZERO;
    o.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
    o.setFeeAmount(fee);
    o.setTotal(subtotal.add(fee).setScale(2, RoundingMode.HALF_UP));
    return orders.save(o);
  }

  public BigDecimal calculateFee(BigDecimal subtotal) {
    return subtotal.multiply(feePercent).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP).add(feeFlat).setScale(2, RoundingMode.HALF_UP);
  }

  @Scheduled(fixedDelay = 600000)
  public void expireAbandonedPaystackOrders() {
    Instant cutoff = Instant.now().minusSeconds(1800);
    for (Order o : orders.findByPaymentMethodAndPaymentStatusAndCreatedAtBefore("PAYSTACK", "PENDING", cutoff)) {
      restoreStock(o, "ORDER_EXPIRED");
      o.setStatus("EXPIRED"); o.setUpdatedAt(Instant.now()); orders.save(o);
    }
  }

  @Transactional
  public Order cancelPendingOrder(String orderId) {
    Order o = orders.findById(orderId).orElseThrow(() -> new IllegalArgumentException("Order not found"));
    boolean cancellable = "PAYSTACK".equals(o.getPaymentMethod()) && "PENDING".equals(o.getPaymentStatus())
        && !"COMPLETED".equals(o.getStatus()) && !"CANCELLED".equals(o.getStatus()) && !"EXPIRED".equals(o.getStatus());
    if (!cancellable) throw new IllegalStateException("Order cannot be cancelled");
    restoreStock(o, "ORDER_CANCELLED");
    o.setStatus("CANCELLED"); o.setUpdatedAt(Instant.now());
    return orders.save(o);
  }

  private void restoreStock(Order o, String reason) {
    for (OrderItem i : o.getItems()) {
      Product updated;
      if (i.getSize() != null && !i.getSize().isBlank()) {
        Query q = new Query(Criteria.where("_id").is(i.getProductId()));
        Update u = new Update().inc("stock", i.getQuantity()).inc("sizes.$[elem].stock", i.getQuantity())
            .filterArray(Criteria.where("elem.size").is(i.getSize())).set("updatedAt", Instant.now());
        updated = mongo.findAndModify(q, u, FindAndModifyOptions.options().returnNew(true), Product.class);
      } else {
        Query q = new Query(Criteria.where("_id").is(i.getProductId()));
        Update u = new Update().inc("stock", i.getQuantity()).set("updatedAt", Instant.now());
        updated = mongo.findAndModify(q, u, FindAndModifyOptions.options().returnNew(true), Product.class);
      }
      if (updated != null) {
        InventoryTransaction it = new InventoryTransaction();
        it.setProductId(i.getProductId()); it.setOrderId(o.getId()); it.setOrderNumber(o.getOrderNumber());
        it.setCustomerName(o.getCustomerName()); it.setSize(i.getSize());
        it.setQuantityDelta(i.getQuantity()); it.setBalanceAfter(updated.getStock()); it.setReason(reason);
        tx.save(it);
      }
    }
  }

  public record Line(@NotBlank String productId, @Positive int quantity, String size) {}
  public record CreateOrderRequest(@NotBlank @Size(max = 120) String customerName, @Email @NotBlank @Size(max = 180) String email,
      @NotBlank @Size(max = 30) String phone, @Size(max = 400) String address, String paymentMethod, @NotEmpty List<Line> items) {}
}
