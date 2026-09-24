package com.toabea.closet.common;
import lombok.*; import org.springframework.data.annotation.*; import org.springframework.data.mongodb.core.index.Indexed; import org.springframework.data.mongodb.core.mapping.Document; import java.time.Instant;
@Document("customers") @Getter @Setter @NoArgsConstructor
public class Customer { @Id private String id; @Getter @Indexed(unique=true,sparse=true) private String email; private String name; private String phone; private String address; private Instant createdAt=Instant.now(); private Instant updatedAt=Instant.now(); }
