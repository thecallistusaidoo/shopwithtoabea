package com.toabea.closet.settings;
import jakarta.validation.Valid; import jakarta.validation.constraints.*; import org.springframework.web.bind.annotation.*; import java.time.Instant;
@RestController @RequestMapping("/api/settings") public class SettingsController {
  private final SettingsRepository repo;
  public SettingsController(SettingsRepository repo){this.repo=repo;}
  public record SettingsRequest(@NotBlank @Pattern(regexp="\\d{9,15}") String whatsappNumber){}
  @GetMapping public Settings get(){return repo.findById(Settings.SINGLETON_ID).orElseGet(Settings::new);}
  @PutMapping public Settings update(@RequestBody @Valid SettingsRequest r){
    Settings s=repo.findById(Settings.SINGLETON_ID).orElseGet(Settings::new);
    s.setWhatsappNumber(r.whatsappNumber());
    s.setUpdatedAt(Instant.now());
    return repo.save(s);
  }
}
