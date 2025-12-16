package com.lingo.lingoproject.security.form;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.exception.ErrorCode;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.security.controller.dto.LoginInfoDto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.authentication.AuthenticationFilter;


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
    if (request.getRequestURI().equals("/login") && request.getMethod().equalsIgnoreCase("POST")){
      // body 값 조회
      String requestBody = request.getReader()
          .lines()
          .collect(Collectors.joining(System.lineSeparator()));

      // body 값 역직렬화
      LoginInfoDto info = null;
      try {
        info = objectMapper.readValue(requestBody, LoginInfoDto.class);
        request.setAttribute("requestBody", info);
      } catch (Exception e) {
        log.error("uri={}, loc=AuthenticationFilter, step=로그인_요청_역직렬화, status=FAILED", request.getRequestURI(), e);
        throw new RingoException(e.getMessage(), ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
      }

      // 아이디&비밀번호 인증
      Authentication authentication = customAuthenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(info.email(), info.password(), new ArrayList<>())
      );
      SecurityContextHolder.getContext().setAuthentication(authentication);

    }

    filterChain.doFilter(request, response);
  }

}
