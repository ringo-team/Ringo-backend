package com.lingo.lingoproject.shared.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingo.lingoproject.shared.exception.ExceptionHandlerFilter;
import com.lingo.lingoproject.shared.infrastructure.persistence.BlockedUserRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.UserRepository;
import com.lingo.lingoproject.shared.security.form.CustomAuthenticationFilter;
import com.lingo.lingoproject.shared.security.form.CustomAuthenticationManager;
import com.lingo.lingoproject.shared.security.form.CustomAuthenticationProvider;
import com.lingo.lingoproject.user.infrastructure.security.CustomUserDetailService;
import com.lingo.lingoproject.shared.security.jwt.JwtAuthenticationFilter;
import com.lingo.lingoproject.shared.security.jwt.JwtUtil;
import com.lingo.lingoproject.shared.utils.RedisUtils;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationConverter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Spring Security 전체 설정 클래스.
 *
 * <h2>필터 체인 순서 (요청이 통과하는 순서)</h2>
 * <pre>
 * HTTP 요청
 *   → ExceptionHandlerFilter        : 필터 계층에서 발생한 예외를 JSON 응답으로 변환
 *   → CustomAuthenticationFilter    : POST /login — ID/PW 인증 처리
 *   → JwtAuthenticationFilter       : Bearer 토큰 검증 및 SecurityContext 세팅
 *   → UsernamePasswordAuthenticationFilter (Spring 기본, 실질적으로 비활성화됨)
 *   → Controller
 * </pre>
 *
 * <h2>인증 방식</h2>
 * <ul>
 *   <li>폼 로그인 비활성화 — JSON Body 기반 ID/PW 로그인 사용 ({@link CustomAuthenticationFilter})</li>
 *   <li>세션 비활성화 (STATELESS) — JWT 토큰 기반 인증</li>
 *   <li>CSRF 비활성화 — REST API 서버이므로 불필요</li>
 * </ul>
 *
 * <h2>인가 규칙</h2>
 * <ul>
 *   <li>whiteList: 인증 없이 접근 가능한 경로</li>
 *   <li>/admin/**: ADMIN 역할만 접근 가능</li>
 *   <li>/photographers/**: PHOTOGRAPHER 역할만 접근 가능</li>
 *   <li>그 외: 인증된 사용자만 접근 가능</li>
 * </ul>
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

  private final ObjectMapper objectMapper;
  private final JwtUtil jwtUtil;
  private final RedisUtils redisUtils;
  private final UserRepository userRepository;
  private final BlockedUserRepository blockedUserRepository;
  private final CustomUserDetailService customUserDetailService;

  /**
   * 인증 없이 접근 가능한 경로 목록 (화이트리스트).
   * - /signup/**: 회원가입 단계별 API
   * - /users/access: 로그인 ID 중복 확인
   * - /health: 서버 헬스체크 (로드밸런서용)
   * - /swagger-ui/**, v3/api-docs/**: Swagger UI
   * - /ws, /stomp/**: WebSocket 연결 (토큰은 STOMP 헤더로 별도 검증)
   * - /actuator/**: Spring Actuator (모니터링)
   * - /profiles, /feeds: 비로그인 상태에서도 프로필/피드 열람 가능
   */
  private final String[] whiteList = {
      "/signup/**", "/users/access", "/health",
      "/swagger-ui/**", "/v3/api-docs/**",
      "/ws", "/stomp/**", "/stomp-test.html",
      "/actuator/**",
      "/profiles", "/feeds"
  };

  /**
   * 비밀번호 인코더. BCrypt 알고리즘을 사용합니다.
   * 회원가입 및 로그인 시 비밀번호 해싱/검증에 사용됩니다.
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * Security 필터 체인 구성.
   *
   * <p>addFilterBefore 순서에 주의하세요.
   * Spring Security는 addFilterBefore(A, B)를 "A를 B 앞에 삽입"으로 처리하므로,
   * 실제 실행 순서는 등록 선언의 역순입니다:
   * ExceptionHandlerFilter → CustomAuthenticationFilter → JwtAuthenticationFilter</p>
   */
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http,
      RedisTemplate<String, Object> redisTemplate) throws Exception {
    http
        // CORS 설정 적용
        .cors((cors) -> cors.configurationSource(corsConfigurationSource()))
        // 경로별 인가 규칙
        .authorizeHttpRequests(authorize ->
            authorize
                .requestMatchers(whiteList).permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/photographers/**").hasRole("PHOTOGRAPHER")
                .anyRequest().authenticated())
        // 폼 로그인 비활성화 (JSON 기반 커스텀 필터 사용)
        .formLogin(AbstractHttpConfigurer::disable)
        // CSRF 비활성화 (REST API)
        .csrf(AbstractHttpConfigurer::disable)
        // JWT 토큰 검증 필터 등록
        .addFilterBefore(new JwtAuthenticationFilter(jwtUtil, redisTemplate, userRepository, blockedUserRepository), UsernamePasswordAuthenticationFilter.class)
        // ID/PW 로그인 처리 필터 (JWT 필터 앞에 위치)
        .addFilterBefore(new CustomAuthenticationFilter(customAuthenticationManager(), objectMapper, authenticationConverter()), JwtAuthenticationFilter.class)
        // 필터 계층 예외 핸들러 (가장 먼저 실행되어야 함)
        .addFilterBefore(new ExceptionHandlerFilter(), CustomAuthenticationFilter.class)
        // 세션 미사용 (JWT Stateless)
        .sessionManagement(session -> session.sessionCreationPolicy(
            SessionCreationPolicy.STATELESS
        ));
    return http.build();
  }

  /**
   * CORS 설정. 현재 모든 Origin 허용 (*).
   * 운영 배포 시 실제 프론트엔드 도메인으로 제한해야 합니다.
   * (주석 처리된 setAllowCredentials(true) 사용 시 allowedOrigin("*") 불가 — 명시적 도메인 지정 필요)
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();

    //config.setAllowCredentials(true);
    config.addAllowedOrigin("*"); // https://ringo.linkgo.com 도 추가
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
    config.setAllowedHeaders(List.of("*"));
    config.setExposedHeaders(List.of("*"));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  /**
   * 커스텀 인증 매니저. ID/PW 인증 요청을 {@link CustomAuthenticationProvider}에 위임합니다.
   */
  @Bean
  public CustomAuthenticationManager customAuthenticationManager() {
    return new CustomAuthenticationManager(authenticationProvider());
  }

  /**
   * 커스텀 인증 프로바이더.
   * UserDetails 로드 → BCrypt 비밀번호 검증 순서로 동작합니다.
   */
  @Bean
  public CustomAuthenticationProvider authenticationProvider(){
    return new CustomAuthenticationProvider(customUserDetailService, passwordEncoder());
  }

  /**
   * 인증 컨버터. BasicAuthenticationConverter를 기반으로 사용하되
   * {@link CustomAuthenticationFilter}에서 JSON Body 파싱을 직접 처리합니다.
   */
  @Bean
  public AuthenticationConverter authenticationConverter(){
    return new BasicAuthenticationConverter();
  }
}
