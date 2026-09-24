package com.toabea.closet.config;
import com.toabea.closet.settings.*; import org.springframework.beans.factory.annotation.Value; import org.springframework.web.bind.annotation.*; import java.math.BigDecimal;

@RestController
@RequestMapping("/api/config")
public class PublicConfigController {
  private final SettingsRepository settings;
  private final String paystackPublicKey;
  private final String cloudinaryCloudName;
  private final BigDecimal feePercent;
  private final BigDecimal feeFlat;

  public PublicConfigController(SettingsRepository settings,
      @Value("${app.paystack.public-key:}") String paystackPublicKey,
      @Value("${app.cloudinary.cloud-name:}") String cloudinaryCloudName,
      @Value("${app.paystack.fee-percent:1.95}") BigDecimal feePercent,
      @Value("${app.paystack.fee-flat:0}") BigDecimal feeFlat){
    this.settings=settings;this.paystackPublicKey=paystackPublicKey;this.cloudinaryCloudName=cloudinaryCloudName;this.feePercent=feePercent;this.feeFlat=feeFlat;
  }

  public record PublicConfig(String paystackPublicKey,String cloudinaryCloudName,BigDecimal feePercent,BigDecimal feeFlat,String whatsappNumber){}

  @GetMapping("/public")
  public PublicConfig publicConfig(){
    Settings s=settings.findById(Settings.SINGLETON_ID).orElseGet(Settings::new);
    return new PublicConfig(paystackPublicKey,cloudinaryCloudName,feePercent,feeFlat,s.getWhatsappNumber());
  }
}
