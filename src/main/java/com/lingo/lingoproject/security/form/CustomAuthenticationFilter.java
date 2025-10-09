package com.lingo.lingoproject.security.form;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingo.lingoproject.security.controller.LoginInfoDto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@WebFilter("/login")
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class CustomAuthenticationFilter extends OncePerRequestFilter {

  private final CustomAuthenticationManager customAuthenticationManager;
  private final ObjectMapper objectMapper;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    if(request.getRequestURI().equals("/login") && request.getMethod().equalsIgnoreCase("POST")){
      String requestBody = request.getReader()
          .lines()
          .collect(Collectors.joining(System.lineSeparator()));
      LoginInfoDto info = null;

      try {
        info = objectMapper.readValue(requestBody, LoginInfoDto.class);
        request.setAttribute("requestBody", info);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      Authentication authentication = customAuthenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(info.email(), info.password(),
              new ArrayList<>())
      );
      SecurityContextHolder.getContext().setAuthentication(authentication);
    }
    filterChain.doFilter(request, response);
  }
}
