package com.lingo.lingoproject.security.jwt;



import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.repository.JwtTokenRepository;
import com.lingo.lingoproject.repository.UserRepository;
import com.lingo.lingoproject.security.TokenType;
import com.lingo.lingoproject.security.util.RandomUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {
  @Value("${jwt.issuer}")
  private String issuer;

  @Value("${jwt.secret}")
  private String secret;

  @Value("${jwt.token.expiration}")
  private String accessTokenExpiration;

  @Value("${jwt.refresh.expiration}")
  private String refreshTokenExpiration;

  private final JwtTokenRepository jwtTokenRepository;
  private final UserRepository userRepository;
  private final RandomUtil randomUtil;

  public String generateToken(TokenType classification, String username, int rand){
    Long expiration = null;
    if (classification == TokenType.ACCESS) {
      expiration = Long.parseLong(accessTokenExpiration);
    }
    else if(classification == TokenType.REFRESH){
      expiration = Long.parseLong(refreshTokenExpiration);
    }
    else{
      throw new IllegalArgumentException("Invalid token type.");
    }
    log.info(username);
    Optional<User> user = userRepository.findByEmail(username);
    if (user.isEmpty()){
      throw new IllegalArgumentException("Invalid username or password.");
    }
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", user.get().getId());
    claims.put("rand", rand);
    return Jwts.builder()
        .issuer("lingo")
        .claims(claims)
        .issuedAt(new Date())
        .subject(username)
        .expiration(new Date(System.currentTimeMillis() + expiration))
        .signWith(this.getSigningKey())
        .compact();
  }

  private SecretKey getSigningKey(){
    byte[] keyBytes = Decoders.BASE64.decode(secret);
    return Keys.hmacShaKeyFor(keyBytes);
  }

  public Claims getClaims(String token){
    try{
      return Jwts.parser().verifyWith(getSigningKey())
          .build()
          .parseSignedClaims(token)
          .getPayload();
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  /*
   유효한 토큰인지를 검증
   */
  public boolean validateToken(String token){
    try {
      Claims claims = getClaims(token);
      /*
       토큰의 유효시간이 지나면 유효하지 않은 토큰으로 간주
       */
      Date expirationTime = claims.getExpiration();
      Date now = new Date();
      if (now.before(expirationTime)){
        return true;
      }
      return false;
    }catch (Exception e){
      return false;
    }
  }

  /*
   정말 유저가 발급한 토큰인지를 검증
   */
  public boolean isAuthenticatedToken(String token){
      try {
        Claims claims = getClaims(token);
        /*
         토큰에 저장된 userId가 데이터베이스에 존재하지 않을 경우 유효하지 않은 토큰으로 간주
         */
        Optional<User> user = userRepository.findById((Long) claims.get("userId"));
        if (user.isEmpty()) {
          return false;
        }
        /*
         저장된 random 값과 일치하지 않는 경우 유효하지 않는 토큰으로 간주
        */
        int rand = (int) claims.get("rand");
        int storedRand = jwtTokenRepository.findByUser(user.get()).getRand();
        if (storedRand != rand) {
          return false;
        }
        return true;
      } catch (Exception e) {
          return false;
      }
  }

  public boolean validateRefreshToken(String token){
    try{
      Claims claims = getClaims(token);
      /*
       토큰의 유효시간이 지나면 유효하지 않은 토큰으로 간주
       */
      Date expirationTime = claims.getExpiration();
      Date now = new Date();
      if (now.before(expirationTime)){
        return true;
      }
      return false;
    }catch (Exception e){
      return false;
    }
  }
}
