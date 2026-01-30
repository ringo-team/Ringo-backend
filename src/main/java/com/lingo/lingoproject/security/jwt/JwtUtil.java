package com.lingo.lingoproject.security.jwt;



import com.lingo.lingoproject.domain.JwtRefreshToken;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.exception.ErrorCode;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.repository.JwtRefreshTokenRepository;
import com.lingo.lingoproject.security.TokenType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
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
    Long expiration = switch (tokenType) {
      case TokenType.ACCESS -> Long.parseLong(accessTokenExpiration);
      case TokenType.REFRESH -> Long.parseLong(refreshTokenExpiration);
      default -> throw new RingoException(
          "토큰 타입은 access, refresh 둘 중 하나입니다.", ErrorCode.BAD_PARAMETER, HttpStatus.BAD_REQUEST);
    };
    return Jwts.builder()
        .issuer(issuer)
        .issuedAt(new Date())
        .subject(user.getLoginId())
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
    } catch (ExpiredJwtException e){
      log.error("유효기간이 지난 토큰입니다", e);
      throw new RingoException("유효기간이 지난 토큰입니다.", ErrorCode.TOKEN_EXPIRED, HttpStatus.UNAUTHORIZED);
    } catch (Exception e){
      log.error("토큰이 유효하지 않습니다.", e);
      throw new RingoException("토큰이 유효하지 않습니다.", ErrorCode.TOKEN_INVALID, HttpStatus.BAD_REQUEST);
    }
  }

  public boolean hasJwtRefreshToken(User user){
    return jwtRefreshTokenRepository.existsByUser(user);
  }

  public void saveJwtRefreshToken(User user, String refreshToken){
    JwtRefreshToken jwtRefreshToken = jwtRefreshTokenRepository
        .findByUser(user).orElseThrow(() -> new RingoException("리프레시 토큰 객체가 없습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR));
    jwtRefreshToken.setRefreshToken(refreshToken);
    jwtRefreshTokenRepository.save(jwtRefreshToken);
  }
}
