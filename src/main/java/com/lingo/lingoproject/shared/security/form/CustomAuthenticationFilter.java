package com.lingo.lingoproject.shared.security.form;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import com.lingo.lingoproject.user.presentation.dto.LoginInfoDto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.authentication.AuthenticationFilter;


/**
 * JSON Body 기반 ID/PW 로그인을 처리하는 커스텀 인증 필터.
 *
 * <h2>동작 원리</h2>
 * <p>Spring Security의 기본 {@link AuthenticationFilter}를 확장합니다.
 * POST /login 요청이 오면 Request Body를 JSON으로 파싱하여
 * {@link LoginInfoDto}의 loginId, password를 추출한 뒤
 * {@link CustomAuthenticationManager} → {@link CustomAuthenticationProvider}
 * 순서로 인증을 처리합니다.</p>
 *
 * <h2>필터 체인에서의 위치</h2>
 * <pre>
 * ExceptionHandlerFilter
 *   → [이 필터] CustomAuthenticationFilter  ← POST /login 여기서 처리
 *     → JwtAuthenticationFilter             ← POST /login 은 여기서 건너뜀
 * </pre>
 *
 * <h2>로그인 성공 후 토큰 발급</h2>
 * <p>이 필터 자체는 JWT를 발급하지 않습니다.
 * 로그인 응답에서 토큰을 발급하려면 컨트롤러({@code /login} 매핑) 또는
 * {@link org.springframework.security.web.authentication.AuthenticationSuccessHandler}를 활용하세요.</p>
 *
 * <h2>POST /login이 아닌 요청</h2>
 * <p>다른 경로의 요청은 별다른 처리 없이 {@code filterChain.doFilter()}로 넘어갑니다.
 * 그러면 다음 필터인 {@link com.lingo.lingoproject.shared.security.jwt.JwtAuthenticationFilter}가 JWT를 검증합니다.</p>
 */
@Slf4j
public class CustomAuthenticationFilter extends AuthenticationFilter {

  private final CustomAuthenticationManager customAuthenticationManager;
  private final ObjectMapper objectMapper;


  public CustomAuthenticationFilter(CustomAuthenticationManager customAuthenticationManager,
      ObjectMapper objectMapper,
      AuthenticationConverter authenticationConverter) {
    super(customAuthenticationManager, authenticationConverter);

    this.customAuthenticationManager = customAuthenticationManager;
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    if (request.getRequestURI().equalsIgnoreCase("/login") &&
        request.getMethod().equalsIgnoreCase("POST")){
      // body 값 조회: InputStream은 한 번만 읽을 수 있으므로 lines()로 모아서 처리
      String requestBody = request.getReader()
          .lines()
          .collect(Collectors.joining(System.lineSeparator()));

      // body 값 역직렬화: {"loginId": "...", "password": "..."} 형식
      LoginInfoDto info;
      try {
        info = objectMapper.readValue(requestBody, LoginInfoDto.class);
      } catch (Exception e) {
        log.error("step=로그인_요청_역직렬화_실패, uri={}", request.getRequestURI(), e);
        throw new RingoException(e.getMessage(), ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
      }

      // 아이디&비밀번호 인증: CustomAuthenticationProvider에서 UserDetails 조회 후 BCrypt 비밀번호 검증
      Authentication authentication = customAuthenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(info.loginId(), info.password(), new ArrayList<>())
      );
      SecurityContextHolder.getContext().setAuthentication(authentication);

    }

    filterChain.doFilter(request, response);
  }

}
