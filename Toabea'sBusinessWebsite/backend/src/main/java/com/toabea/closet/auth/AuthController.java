package com.toabea.closet.auth;
import com.toabea.closet.common.*; import jakarta.servlet.http.HttpServletRequest; import org.springframework.http.*; import org.springframework.security.authentication.*; import org.springframework.security.core.Authentication; import org.springframework.security.core.AuthenticationException; import org.springframework.security.core.context.SecurityContextHolder; import org.springframework.security.crypto.password.PasswordEncoder; import org.springframework.web.bind.annotation.*; import org.springframework.web.server.ResponseStatusException; import java.time.Instant; import java.util.*; import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private static final int MAX_ATTEMPTS=5;
  private static final long WINDOW_SECONDS=900;
  private final Map<String,Deque<Instant>> attemptsByIp=new ConcurrentHashMap<>();

  private final AuthenticationManager auth;
  private final JwtService jwt;
  private final AdminUserRepository repo;
  private final PasswordEncoder encoder;

  public AuthController(AuthenticationManager a,JwtService j,AdminUserRepository r,PasswordEncoder e){auth=a;jwt=j;repo=r;encoder=e;}

  @PostMapping("/login")
  public AuthModels.LoginResponse login(@RequestBody AuthModels.LoginRequest req,HttpServletRequest request){
    String ip=clientIp(request);
    checkRateLimit(ip);
    try{
      auth.authenticate(new UsernamePasswordAuthenticationToken(req.username(),req.password()));
    }catch(AuthenticationException e){
      recordAttempt(ip);
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid username or password");
    }
    return new AuthModels.LoginResponse(jwt.issue(repo.findByUsername(req.username()).orElseThrow()),req.username());
  }

  public record ChangePasswordRequest(String currentPassword,String newPassword){}

  @PatchMapping("/password")
  public ApiModels.MessageResponse changePassword(@RequestBody ChangePasswordRequest req){
    if(req.newPassword()==null||req.newPassword().length()<8)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"New password must be at least 8 characters");
    Authentication current=SecurityContextHolder.getContext().getAuthentication();
    AdminUser user=repo.findByUsername(current.getName()).orElseThrow();
    if(req.currentPassword()==null||!encoder.matches(req.currentPassword(),user.getPasswordHash()))
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Current password is incorrect");
    user.setPasswordHash(encoder.encode(req.newPassword()));
    repo.save(user);
    return new ApiModels.MessageResponse("Password updated");
  }

  private void checkRateLimit(String ip){
    Deque<Instant> attempts=attemptsByIp.computeIfAbsent(ip,k->new ArrayDeque<>());
    synchronized(attempts){
      Instant cutoff=Instant.now().minusSeconds(WINDOW_SECONDS);
      while(!attempts.isEmpty()&&attempts.peekFirst().isBefore(cutoff))attempts.pollFirst();
      if(attempts.size()>=MAX_ATTEMPTS)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Too many login attempts, try again later");
    }
  }

  private void recordAttempt(String ip){
    Deque<Instant> attempts=attemptsByIp.computeIfAbsent(ip,k->new ArrayDeque<>());
    synchronized(attempts){attempts.addLast(Instant.now());}
  }

  private String clientIp(HttpServletRequest request){
    // server.forward-headers-strategy=framework already resolves the real client address here
    // when running behind a trusted reverse proxy; do not re-trust client-supplied headers directly.
    return request.getRemoteAddr();
  }
}
