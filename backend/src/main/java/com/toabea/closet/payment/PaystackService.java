package com.toabea.closet.payment;
import com.fasterxml.jackson.databind.*; import org.springframework.beans.factory.annotation.Value; import org.springframework.stereotype.Service; import javax.crypto.Mac; import javax.crypto.spec.SecretKeySpec; import java.net.URI; import java.net.http.*; import java.nio.charset.StandardCharsets; import java.security.MessageDigest; import java.time.Duration;

@Service
public class PaystackService {
  private final String secretKey;
  private final ObjectMapper mapper=new ObjectMapper();
  private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  public PaystackService(@Value("${app.paystack.secret-key:}") String secretKey){this.secretKey=secretKey;}

  public record VerifyResult(boolean success,String status,long amountPesewas,String currency,String orderId){}

  public VerifyResult verifyTransaction(String reference){
    if(secretKey==null||secretKey.isBlank())throw new IllegalStateException("Paystack secret key is not configured");
    try{
      HttpRequest req=HttpRequest.newBuilder(URI.create("https://api.paystack.co/transaction/verify/"+java.net.URLEncoder.encode(reference,StandardCharsets.UTF_8)))
        .header("Authorization","Bearer "+secretKey).timeout(Duration.ofSeconds(15)).GET().build();
      HttpResponse<String> res=http.send(req,HttpResponse.BodyHandlers.ofString());
      JsonNode root=mapper.readTree(res.body());
      boolean apiOk=res.statusCode()==200 && root.path("status").asBoolean(false);
      if(!apiOk)return new VerifyResult(false,"api_error",0,"",null);
      JsonNode data=root.path("data");
      String status=data.path("status").asText("");
      long amount=data.path("amount").asLong(0);
      String currency=data.path("currency").asText("");
      String orderId=data.path("metadata").path("orderId").asText(null);
      return new VerifyResult("success".equalsIgnoreCase(status),status,amount,currency,orderId);
    }catch(Exception e){
      return new VerifyResult(false,"error",0,"",null);
    }
  }

  public boolean isValidWebhookSignature(String rawBody,String signatureHeader){
    if(secretKey==null||secretKey.isBlank()||signatureHeader==null||signatureHeader.isBlank())return false;
    try{
      Mac mac=Mac.getInstance("HmacSHA512");
      mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8),"HmacSHA512"));
      byte[] computed=mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
      String computedHex=bytesToHex(computed);
      return MessageDigest.isEqual(computedHex.getBytes(StandardCharsets.UTF_8),signatureHeader.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
    }catch(Exception e){
      return false;
    }
  }

  public String extractReferenceFromWebhook(String rawBody){
    try{
      JsonNode root=mapper.readTree(rawBody);
      return root.path("data").path("reference").asText(null);
    }catch(Exception e){
      return null;
    }
  }

  private static String bytesToHex(byte[] bytes){
    StringBuilder sb=new StringBuilder(bytes.length*2);
    for(byte b:bytes)sb.append(String.format("%02x",b));
    return sb.toString();
  }
}
