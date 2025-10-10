package com.lingo.lingoproject.security.jwt;



import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.repository.JwtRefreshTokenRepository;
import com.lingo.lingoproject.repository.UserRepository;
import com.lingo.lingoproject.security.TokenType;
import com.lingo.lingoproject.security.util.RandomUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
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

  private final JwtRefreshTokenRepository jwtRefreshTokenRepository;
  private final UserRepository userRepository;
  private final RandomUtil randomUtil;

  public String generateToken(TokenType classification, String username){
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
  public boolean isValidToken(String token){
    try {
      Jwts.parser().verifyWith(getSigningKey())
          .build()
          .parseSignedClaims(token);
    }catch (Exception e){
      return false;
    }
    return true;
  }

  /**
   * 유효기간이 지난 코드의 경우 true를 반환함
   * 유효기간이 지나지 않았거나 그 외 오류가 발생할 시 false를 반환함
   * @param token
   * @return
   */
  public boolean isExpiredToken(String token){
    try{
      Claims claims = getClaims(token);
      return claims.getExpiration().before(new Date());
    }catch(ExpiredJwtException e){
      return true;
    }catch (Exception e){
      return false;
    }
  }

}
