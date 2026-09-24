package com.toabea.closet.config;
import org.springframework.beans.factory.annotation.Value; import org.springframework.context.annotation.*; import org.springframework.data.mongodb.MongoDatabaseFactory; import org.springframework.data.mongodb.MongoTransactionManager; import org.springframework.web.cors.*; import java.util.*;
@Configuration public class AppConfig { @Bean MongoTransactionManager transactionManager(MongoDatabaseFactory f){return new MongoTransactionManager(f);}
@Bean CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins}") String origins){
  CorsConfiguration config=new CorsConfiguration();
  config.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).filter(o->!o.isBlank()).toList());
  config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
  config.setAllowedHeaders(List.of("*"));
  config.setAllowCredentials(false);
  config.setMaxAge(3600L);
  UrlBasedCorsConfigurationSource source=new UrlBasedCorsConfigurationSource();
  source.registerCorsConfiguration("/api/**",config);
  return source;
}
}
