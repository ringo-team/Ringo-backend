package com.lingo.lingoproject.shared.security.jwt;


import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.domain.model.SignupStatus;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import com.lingo.lingoproject.shared.infrastructure.persistence.BlockedUserRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT 토큰 기반 인증 필터. 요청당 한 번만 실행됩니다 ({@link OncePerRequestFilter}).
 *
 * <h2>처리 흐름</h2>
 * <ol>
 *   <li>Authorization 헤더에서 Bearer 토큰 추출</li>
 *   <li>{@link JwtUtil#getClaims}로 서명/만료 검증</li>
 *   <li>Redis 블랙리스트 확인 — 로그아웃한 유저 차단 ({@code logoutUser::{jti}})</li>
 *   <li>Redis 정지 목록 확인 — 일시 정지된 유저 차단 ({@code suspension::{userId}})</li>
 *   <li>DB 영구 정지 확인 — {@link BlockedUserRepository}</li>
 *   <li>회원가입 미완료 유저 차단 (status != COMPLETED)</li>
 *   <li>활동 시간 추적 — Redis에 {@code connect-app::{userId}::{timestamp}} 키를 30분 TTL로 갱신</li>
 *   <li>SecurityContext에 인증 객체 등록</li>
 * </ol>
 *
 * <h2>활동 시간 추적 메커니즘</h2>
 * <p>API 요청이 올 때마다 {@code connect-app::{userId}::{시작시간}} 키를 30분 TTL로 갱신합니다.
 * 30분간 요청이 없으면 키가 만료되고, Redis Keyspace 이벤트로
 * {@link com.lingo.lingoproject.shared.config.RedisExpireListener}가 활동 기록을 DB에 저장합니다.</p>
 *
 * <h2>예외 사항</h2>
 * <p>POST /login은 이 필터를 거치지 않고 바로 다음 필터({@link com.lingo.lingoproject.shared.security.form.CustomAuthenticationFilter})로 넘어갑니다.
 * 토큰이 없는 요청은 whiteList에 있는 경우에만 이후 필터로 통과됩니다.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final JwtUtil jwtUtil;
  private final RedisTemplate<String, Object> redisTemplate;
  private final UserRepository userRepository;
  private final BlockedUserRepository blockedUserRepository;


  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    String accessToken = request.getHeader("Authorization");

    // /login 요청은 CustomAuthenticationFilter가 처리하므로 이 필터에서는 건너뜀
    if (request.getRequestURI().equalsIgnoreCase("/login")){
      filterChain.doFilter(request, response);
      return;
    }

    else if (accessToken != null && accessToken.startsWith("Bearer ")) {
      // "Bearer " 접두사(7자)를 제거하여 순수 토큰 문자열 추출
      accessToken = accessToken.substring(7);

      // 서명 검증 및 claims 파싱 (만료/위변조 시 RingoException 발생)
      Claims claims = jwtUtil.getClaims(accessToken);
      User user = userRepository.findByLoginId(claims.getSubject())
          .orElseThrow(() -> new RingoException("유효하지 않은 토큰입니다.", ErrorCode.TOKEN_INVALID, HttpStatus.FORBIDDEN));

      // 로그아웃한 유저가 기존 토큰으로 접근하려고 할때 접근을 차단함
      // 로그아웃 시 해당 토큰의 jti를 Redis에 등록 (TTL = 토큰 잔여 만료 시간)
      if(redisTemplate.hasKey("logoutUser::" + claims.getId())){
        throw new RingoException("유효하지 않은 토큰 입니다.", ErrorCode.LOGOUT, HttpStatus.FORBIDDEN);
      }

      // 계정 정지된 사람일 경우 접근을 차단함
      // ReportService.suspendUser()에서 "suspension::{userId}" 키를 TTL과 함께 Redis에 등록
      if (redisTemplate.hasKey("suspension::" + user.getId())){
        throw new RingoException("계정이 정지된 유저입니다.", ErrorCode.BLOCKED, HttpStatus.FORBIDDEN);
      }

      // 영구 정지된 사람인 경우 접근을 차단함 (DB 조회)
      if (blockedUserRepository.existsByBlockedUserId(user.getId())) {
        throw new RingoException("영구정지된 유저입니다.", ErrorCode.BLOCKED, HttpStatus.FORBIDDEN);
      }

      // 회원가입을 마치치 않은 회원의 경우 접근을 차단함
      // /signup/** 경로와 공개 조회 엔드포인트는 예외적으로 허용
      if(!(request.getRequestURI().startsWith("/signup") ||
          request.getRequestURI().equals("/profiles") ||
          request.getRequestURI().equals("/feeds") ||
          user.getStatus().equals(SignupStatus.COMPLETED))){
        throw new RingoException("회원가입을 마치고 요청 주시길 바랍니다.", ErrorCode.BEFORE_SIGNUP ,HttpStatus.FORBIDDEN);
      }

      log.info("userId={}, endpoint={}, step=api_요청", user.getId(), request.getRequestURI());

      // 활동 시간 추적: 기존 키가 있으면 TTL을 30분으로 갱신, 없으면 새 키 생성
      // 키 만료 시 RedisExpireListener가 UserActivityLog를 DB에 저장
      Set<String> keys = redisTemplate.keys("connect-app::" + user.getId() + "*");
      if (!keys.isEmpty()){
        String value = keys.iterator().next();
        redisTemplate.opsForValue().set(value, true, 30, TimeUnit.MINUTES);
      }
      else redisTemplate.opsForValue().set("connect-app::" + user.getId() + "::" + LocalDateTime.now(), true, 30, TimeUnit.MINUTES);

      // SecurityContext에 인증 객체 등록 → 이후 컨트롤러에서 @AuthenticationPrincipal로 주입 가능
      SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken(user, "password", user.getAuthorities())
      );
    }

    filterChain.doFilter(request, response);
  }
}
