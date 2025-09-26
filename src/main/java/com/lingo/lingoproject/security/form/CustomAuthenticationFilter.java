package com.lingo.lingoproject.security.form;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingo.lingoproject.security.controller.LoginInfoDto;
import com.lingo.lingoproject.utils.RequestCacheWrapper;
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
  private final RequestCacheWrapper requestCache;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    if(request.getRequestURI().equals("/login") && request.getMethod().equalsIgnoreCase("POST")){
      String requestBody = request.getReader()
          .lines()
          .collect(Collectors.joining(System.lineSeparator()));
      ObjectMapper mapper = new ObjectMapper();
      LoginInfoDto info = null;

      try {
        requestCache.setContent(requestBody);
        info = mapper.readValue(requestBody, LoginInfoDto.class);
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
