package com.toabea.closet.settings;
import lombok.*; import org.springframework.data.annotation.Id; import org.springframework.data.mongodb.core.mapping.Document; import java.time.Instant;
@Document("settings") @Getter @Setter @NoArgsConstructor public class Settings { public static final String SINGLETON_ID="singleton"; @Id private String id=SINGLETON_ID; private String whatsappNumber="233598162907"; private Instant updatedAt=Instant.now(); }
