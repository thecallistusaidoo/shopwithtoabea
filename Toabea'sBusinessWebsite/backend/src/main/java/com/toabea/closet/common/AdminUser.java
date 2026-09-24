package com.toabea.closet.common;
import lombok.*; import org.springframework.data.annotation.*; import org.springframework.data.mongodb.core.index.Indexed; import org.springframework.data.mongodb.core.mapping.Document; import java.time.Instant; import java.util.*;
@Document("admin_users") @Getter @Setter @NoArgsConstructor public class AdminUser { @Id private String id; @Indexed(unique=true) private String username; private String passwordHash; private Set<String> roles=new HashSet<>(Set.of("ADMIN")); private boolean enabled=true; private Instant createdAt=Instant.now(); }
