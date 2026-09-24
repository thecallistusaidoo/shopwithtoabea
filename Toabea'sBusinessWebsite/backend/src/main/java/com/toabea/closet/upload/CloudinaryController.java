package com.toabea.closet.upload;
import org.springframework.beans.factory.annotation.Value; import org.springframework.web.bind.annotation.*; import java.nio.charset.StandardCharsets; import java.security.MessageDigest; import java.time.Instant;

@RestController
@RequestMapping("/api/uploads")
public class CloudinaryController {
  private final String cloudName;
  private final String apiKey;
  private final String apiSecret;
  private final String folder;

  public CloudinaryController(@Value("${app.cloudinary.cloud-name:}") String cloudName,
      @Value("${app.cloudinary.api-key:}") String apiKey,
      @Value("${app.cloudinary.api-secret:}") String apiSecret,
      @Value("${app.cloudinary.folder:toabea-products}") String folder){
    this.cloudName=cloudName;this.apiKey=apiKey;this.apiSecret=apiSecret;this.folder=folder;
  }

  public record SignatureResponse(long timestamp,String signature,String apiKey,String cloudName,String folder){}

  @GetMapping("/signature")
  public SignatureResponse signature(){
    if(cloudName.isBlank()||apiKey.isBlank()||apiSecret.isBlank())throw new IllegalStateException("Cloudinary is not configured");
    long timestamp=Instant.now().getEpochSecond();
    String paramsToSign="folder="+folder+"&timestamp="+timestamp;
    String signature=sha1Hex(paramsToSign+apiSecret);
    return new SignatureResponse(timestamp,signature,apiKey,cloudName,folder);
  }

  private static String sha1Hex(String input){
    try{
      MessageDigest md=MessageDigest.getInstance("SHA-1");
      byte[] digest=md.digest(input.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb=new StringBuilder(digest.length*2);
      for(byte b:digest)sb.append(String.format("%02x",b));
      return sb.toString();
    }catch(Exception e){
      throw new IllegalStateException("Unable to sign upload request",e);
    }
  }
}
