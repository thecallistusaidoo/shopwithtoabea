package com.toabea.closet.common;
import lombok.*; import java.math.BigDecimal;
@Getter @Setter @NoArgsConstructor public class OrderItem { private String productId; private String name; private String size; private BigDecimal unitPrice; private int quantity; private BigDecimal lineTotal; }
