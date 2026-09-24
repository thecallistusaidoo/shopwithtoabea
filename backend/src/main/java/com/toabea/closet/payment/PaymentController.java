package com.toabea.closet.payment;
import com.toabea.closet.common.*; import jakarta.servlet.http.HttpServletRequest; import org.springframework.http.*; import org.springframework.web.bind.annotation.*; import java.io.IOException; import java.math.*; import java.nio.charset.StandardCharsets; import java.time.Instant;

@RestController
@RequestMapping("/api/payments/paystack")
public class PaymentController {
  private final PaystackService paystack;
  private final OrderRepository orders;

  public PaymentController(PaystackService paystack,OrderRepository orders){this.paystack=paystack;this.orders=orders;}

  public record VerifyRequest(String orderId,String reference){}

  @PostMapping("/verify")
  public ResponseEntity<?> verify(@RequestBody VerifyRequest req){
    if(req.orderId()==null||req.reference()==null||req.orderId().isBlank()||req.reference().isBlank())
      return ResponseEntity.badRequest().body(new ApiModels.MessageResponse("orderId and reference are required"));
    Order order=orders.findById(req.orderId()).orElse(null);
    if(order==null)return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiModels.MessageResponse("Order not found"));
    if("PAID".equals(order.getPaymentStatus()))return ResponseEntity.ok(order);
    PaystackService.VerifyResult result=paystack.verifyTransaction(req.reference());
    ResponseEntity<?> failure=applyResult(order,result,req.reference());
    return failure!=null?failure:ResponseEntity.ok(order);
  }

  @PostMapping("/webhook")
  public ResponseEntity<Void> webhook(HttpServletRequest request,@RequestHeader(value="x-paystack-signature",required=false) String signature) throws IOException{
    String rawBody=new String(request.getInputStream().readAllBytes(),StandardCharsets.UTF_8);
    if(!paystack.isValidWebhookSignature(rawBody,signature))return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    String reference=paystack.extractReferenceFromWebhook(rawBody);
    if(reference==null||reference.isBlank())return ResponseEntity.ok().build();
    PaystackService.VerifyResult result=paystack.verifyTransaction(reference);
    if(result.orderId()==null)return ResponseEntity.ok().build();
    Order order=orders.findById(result.orderId()).orElse(null);
    if(order==null||"PAID".equals(order.getPaymentStatus()))return ResponseEntity.ok().build();
    applyResult(order,result,reference);
    return ResponseEntity.ok().build();
  }

  private ResponseEntity<?> applyResult(Order order,PaystackService.VerifyResult result,String reference){
    BigDecimal expectedPesewas=order.getTotal().multiply(BigDecimal.valueOf(100)).setScale(0,RoundingMode.HALF_UP);
    boolean amountMatches=BigDecimal.valueOf(result.amountPesewas()).compareTo(expectedPesewas)==0;
    boolean currencyMatches=result.currency()==null||result.currency().isBlank()||"GHS".equalsIgnoreCase(result.currency());
    if(!result.success()||!amountMatches||!currencyMatches){
      return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(new ApiModels.MessageResponse("Payment could not be verified"));
    }
    order.setPaymentStatus("PAID");
    order.setPaymentReference(reference);
    order.setUpdatedAt(Instant.now());
    orders.save(order);
    return null;
  }
}
