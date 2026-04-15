package com.lingo.lingoproject.shared.security.jwt;



import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import com.lingo.lingoproject.shared.security.TokenType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * JWT 토큰 생성 및 파싱 유틸리티.
 *
 * <h2>토큰 종류</h2>
 * <ul>
 *   <li><b>ACCESS 토큰</b>: API 요청 시 Authorization 헤더에 전달. 만료 시간이 짧음.
 *       application.yml의 {@code jwt.token.expiration} 값(ms 단위)을 사용합니다.</li>
 *   <li><b>REFRESH 토큰</b>: ACCESS 토큰 재발급 시 사용. 만료 시간이 길며
 *       Redis에 {@code redis::refresh::{loginId}} 키로 저장됩니다.
 *       application.yml의 {@code jwt.refresh.expiration} 값(ms 단위)을 사용합니다.</li>
 * </ul>
 *
 * <h2>토큰 구조 (Payload Claims)</h2>
 * <ul>
 *   <li>{@code jti} (id): UUID — 로그아웃 처리 시 Redis에 블랙리스트로 등록하는 데 사용</li>
 *   <li>{@code iss} (issuer): application.yml의 {@code jwt.issuer}</li>
 *   <li>{@code iat} (issuedAt): 발급 시각</li>
 *   <li>{@code sub} (subject): 유저의 loginId</li>
 *   <li>{@code exp} (expiration): 만료 시각</li>
 * </ul>
 *
 * <h2>서명 알고리즘</h2>
 * Base64로 인코딩된 secret을 디코딩하여 HMAC-SHA 키를 생성합니다.
 * application.yml의 {@code jwt.secret} 값이 충분히 길어야 합니다 (256비트 이상 권장).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

  private final RedisTemplate<String, Object> redisTemplate;

  /** JWT 발급자 식별자 (application.yml: jwt.issuer). */
  @Value("${jwt.issuer}")
  private String issuer;

  /** HMAC 서명에 사용하는 Base64 인코딩 시크릿 키 (application.yml: jwt.secret). */
  @Value("${jwt.secret}")
  private String secret;

  /** ACCESS 토큰 유효시간 (ms 단위, application.yml: jwt.token.expiration). */
  @Value("${jwt.token.expiration}")
  private String accessTokenExpiration;

  /** REFRESH 토큰 유효시간 (ms 단위, application.yml: jwt.refresh.expiration). */
  @Value("${jwt.refresh.expiration}")
  private String refreshTokenExpiration;

  /**
   * 토큰 유형과 사용자 정보를 기반으로 JWT를 생성합니다.
   *
   * <p>토큰의 subject는 {@code user.getLoginId()}이며,
   * {@code jti}(JWT ID)는 UUID로 생성됩니다.
   * 로그아웃 시 이 jti를 Redis 블랙리스트({@code logoutUser::{jti}})에 등록하여
   * 이미 발급된 토큰의 재사용을 차단합니다.</p>
   *
   * @param tokenType ACCESS 또는 REFRESH
   * @param user      토큰에 포함될 사용자 엔티티
   * @return 서명된 JWT 문자열
   */
  public String generateToken(TokenType tokenType, User user){
    Long expiration = switch (tokenType) {
      case TokenType.ACCESS -> Long.parseLong(accessTokenExpiration);
      case TokenType.REFRESH -> Long.parseLong(refreshTokenExpiration);
      default -> throw new RingoException(
          "토큰 타입은 access, refresh 둘 중 하나입니다.", ErrorCode.BAD_PARAMETER, HttpStatus.BAD_REQUEST);
    };
    return Jwts.builder()
        .id(UUID.randomUUID().toString())   // jti: 로그아웃 블랙리스트 식별자
        .issuer(issuer)
        .issuedAt(new Date())
        .subject(user.getLoginId())          // sub: loginId를 subject로 사용
        .expiration(new Date(System.currentTimeMillis() + expiration))
        .signWith(this.getSigningKey())
        .compact();
  }

  /**
   * Base64 인코딩된 secret을 디코딩하여 HMAC-SHA 서명 키를 생성합니다.
   * 키 길이에 따라 자동으로 HS256/HS384/HS512가 선택됩니다.
   */
  private SecretKey getSigningKey(){
    byte[] keyBytes = Decoders.BASE64.decode(secret);
    return Keys.hmacShaKeyFor(keyBytes);
  }

  /**
   * JWT 문자열을 파싱하여 Claims(페이로드)를 반환합니다.
   *
   * <p>서명 검증 및 만료 여부를 함께 확인합니다.
   * 만료된 토큰은 {@link ErrorCode#TOKEN_EXPIRED}, 위변조된 토큰은 {@link ErrorCode#TOKEN_INVALID}를 던집니다.</p>
   *
   * @param token Bearer 접두사가 제거된 순수 JWT 문자열
   * @return 파싱된 Claims (sub, jti 등 포함)
   * @throws RingoException 토큰이 만료되었거나 유효하지 않을 경우
   */
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

}
