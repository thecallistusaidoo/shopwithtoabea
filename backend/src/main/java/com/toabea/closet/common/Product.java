package com.toabea.closet.common;
import lombok.*; import org.springframework.data.annotation.*; import org.springframework.data.mongodb.core.index.Indexed; import org.springframework.data.mongodb.core.mapping.Document; import java.math.BigDecimal; import java.time.Instant; import java.util.*;
@Document("products") @Getter @Setter @NoArgsConstructor
public class Product { @Id private String id; @Indexed private String name; @Indexed private String category; private String description; private BigDecimal price; private BigDecimal compareAtPrice; private String imageUrl; private List<String> imageUrls=new ArrayList<>(); private String badge; private boolean active=true; private int stock; private List<ProductSize> sizes=new ArrayList<>(); private Instant createdAt=Instant.now(); private Instant updatedAt=Instant.now();
  @Getter @Setter @NoArgsConstructor @AllArgsConstructor public static class ProductSize { private String size; private int stock; }
}
