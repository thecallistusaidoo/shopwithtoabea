package com.toabea.closet.common;
import org.springframework.data.mongodb.repository.MongoRepository; import java.time.Instant; import java.util.*;
public interface OrderRepository extends MongoRepository<Order,String> { Optional<Order> findByOrderNumber(String orderNumber); List<Order> findAllByOrderByCreatedAtDesc(); List<Order> findByPaymentMethodAndPaymentStatusAndCreatedAtBefore(String paymentMethod,String paymentStatus,Instant cutoff); }
