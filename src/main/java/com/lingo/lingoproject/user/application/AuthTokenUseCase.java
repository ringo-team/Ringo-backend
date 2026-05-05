package com.lingo.lingoproject.user.application;

import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import com.lingo.lingoproject.shared.security.TokenType;
import com.lingo.lingoproject.shared.security.dto.LoginResponseDto;
import com.lingo.lingoproject.shared.security.dto.RegenerateTokenResponseDto;
import com.lingo.lingoproject.shared.security.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * 토큰 발급/재발급/로그아웃 담당 Use Case
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthTokenUseCase {

  private static final String AUTHORIZATION_HEADER_KEY = "Authorization";
  private static final String ACCESS_TOKEN_PREFIX = "Bearer ";

  private final JwtUtil jwtUtil;
  private final UserQueryUseCase userQueryUseCase;
  private final RedisTemplate<String, Object> redisTemplate;

  public LoginResponseDto login(User user) {
    String access = jwtUtil.generateToken(TokenType.ACCESS, user);
    String refresh = jwtUtil.generateToken(TokenType.REFRESH, user);
    redisTemplate.opsForValue().set("redis::refresh::" + user.getLoginId(), refresh, 30, TimeUnit.DAYS);
    return new LoginResponseDto(ErrorCode.SUCCESS.getCode(), user.getId(), access, refresh);
  }

  public RegenerateTokenResponseDto regenerateToken(String refreshToken) {
    Claims claims = jwtUtil.getClaims(refreshToken);
    Object redisToken = redisTemplate.opsForValue().get("redis::refresh::" + claims.getSubject());
    String token = redisToken != null ? redisToken.toString() : null;

    if (token == null)
      throw new RingoException("로그인하지 않은 유저입니다.", ErrorCode.FORBIDDEN);

    log.info("redisRefreshToken: {}, requestRefreshToken: {}", token, refreshToken);

    if (token.equals(refreshToken)) {
      return generateTokenAndSaveRefreshTokenInRedis(claims.getSubject());
    }
    throw new RingoException("유효하지 않은 토큰입니다.", ErrorCode.NO_AUTH);
  }

  public void logout(HttpServletRequest request) {
    String accessToken = request.getHeader(AUTHORIZATION_HEADER_KEY);
    if (!accessToken.startsWith(ACCESS_TOKEN_PREFIX))
      throw new RingoException("토큰이 형식이 잘못되었습니다.", ErrorCode.BAD_REQUEST);

    accessToken = accessToken.substring(7);
    Claims claims = jwtUtil.getClaims(accessToken);

    redisTemplate.delete("redis::refresh::" + claims.getSubject());
    redisTemplate.opsForValue().set("logoutUser::" + claims.getId(), accessToken, 1, TimeUnit.DAYS);
  }

  private RegenerateTokenResponseDto generateTokenAndSaveRefreshTokenInRedis(String loginId) {
    User user = userQueryUseCase.findByLoginId(loginId)
        .orElseThrow(() -> new RingoException("유저를 찾을 수 없습니다.", ErrorCode.BAD_REQUEST));
    String accessToken = jwtUtil.generateToken(TokenType.ACCESS, user);
    String refresh = jwtUtil.generateToken(TokenType.REFRESH, user);
    redisTemplate.opsForValue().set("redis::refresh::" + user.getLoginId(), refresh, 30, TimeUnit.DAYS);
    return new RegenerateTokenResponseDto(ErrorCode.SUCCESS.getCode(), user.getId(), accessToken, refresh);
  }
}
