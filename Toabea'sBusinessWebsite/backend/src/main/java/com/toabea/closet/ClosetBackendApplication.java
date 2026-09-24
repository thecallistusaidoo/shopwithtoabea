package com.toabea.closet;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.logging.Logger;

@SpringBootApplication
@EnableScheduling
public class ClosetBackendApplication {
  private static final Logger log = Logger.getLogger(ClosetBackendApplication.class.getName());
  private static final String DEFAULT_JWT_SECRET = "change-this-development-secret-to-a-random-256-bit-value";

  public static void main(String[] args) { SpringApplication.run(ClosetBackendApplication.class, args); }

  @Bean
  CommandLineRunner verifyProductionSecrets(Environment env, @Value("${app.jwt.secret}") String jwtSecret) {
    return args -> {
      boolean isProd = Arrays.asList(env.getActiveProfiles()).contains("prod");
      boolean weakSecret = jwtSecret == null || jwtSecret.isBlank() || jwtSecret.equals(DEFAULT_JWT_SECRET)
          || jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32;
      if (weakSecret) {
        if (isProd) {
          throw new IllegalStateException("JWT_SECRET must be set to a random value of at least 32 bytes before running with the 'prod' profile.");
        }
        log.severe("JWT_SECRET is missing, default, or shorter than 32 bytes. Set a strong random value in JWT_SECRET before deploying to production.");
      }
    };
  }
}
