package com.lingo.lingoproject.security.jwt;



import com.lingo.lingoproject.domain.JwtRefreshToken;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.repository.JwtRefreshTokenRepository;
import com.lingo.lingoproject.security.TokenType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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

  public String generateToken(TokenType tokenType, User user){
    Long expiration = null;
    if (tokenType == TokenType.ACCESS) {
      expiration = Long.parseLong(accessTokenExpiration);
    }
    else if(tokenType == TokenType.REFRESH){
      expiration = Long.parseLong(refreshTokenExpiration);
    }
    else{
      throw new RingoException("토큰 타입은 access, refresh 둘 중 하나입니다.", HttpStatus.BAD_REQUEST);
    }
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", user.getId());
    return Jwts.builder()
        .issuer(issuer)
        .claims(claims)
        .issuedAt(new Date())
        .subject(user.getEmail())
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
    }
    catch (ExpiredJwtException e){
      throw new RingoException("유효기간이 지난 토큰입니다.", HttpStatus.UNAUTHORIZED);
    }
    catch (Exception e){
      return false;
    }
    return true;
  }

  public void saveRefreshToken(String token, User user){
    JwtRefreshToken jwtRefreshToken = JwtRefreshToken.builder()
        .refreshToken(token)
        .user(user)
        .build();
    jwtRefreshTokenRepository.save(jwtRefreshToken);
  }

}
