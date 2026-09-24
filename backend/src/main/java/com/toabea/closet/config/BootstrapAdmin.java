package com.toabea.closet.config;
import com.toabea.closet.common.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class BootstrapAdmin {
  private static final Logger log = LoggerFactory.getLogger(BootstrapAdmin.class);

  @Bean
  CommandLineRunner seedAdmin(AdminUserRepository repo, PasswordEncoder encoder,
      @Value("${app.bootstrap-admin.username:}") String username,
      @Value("${app.bootstrap-admin.password:}") String password,
      @Value("${app.bootstrap-admin.reset:false}") boolean reset) {
    return args -> {
      if (username == null || username.isBlank() || password == null || password.isBlank()) return;
      var existing = repo.findByUsername(username);
      if (existing.isEmpty()) {
        AdminUser u = new AdminUser();
        u.setUsername(username);
        u.setPasswordHash(encoder.encode(password));
        repo.save(u);
      } else if (reset) {
        AdminUser u = existing.get();
        u.setPasswordHash(encoder.encode(password));
        u.setEnabled(true);
        repo.save(u);
        log.warn("Admin password for '{}' was reset from BOOTSTRAP_ADMIN_PASSWORD. Remove BOOTSTRAP_ADMIN_RESET after signing in.", username);
      }
    };
  }
}
