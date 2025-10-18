package com.lingo.lingoproject.security.form;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingo.lingoproject.domain.User;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Order(2)
@RequiredArgsConstructor
@Component
@Slf4j
public class CustomAuthenticationFilter extends OncePerRequestFilter {

  private final CustomAuthenticationManager customAuthenticationManager;
  private final ObjectMapper objectMapper;
  private final String[] whiteList = {"/signup", "/swagger", "/v3/api-docs", "/swagger-resources", "/ws", "/stomp"};

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    if (request.getRequestURI().equals("/login") && request.getMethod().equalsIgnoreCase("POST")){
      String requestBody = request.getReader()
          .lines()
          .collect(Collectors.joining(System.lineSeparator()));
      LoginInfoDto info = null;

      try {
        info = objectMapper.readValue(requestBody, LoginInfoDto.class);
        request.setAttribute("requestBody", info);
      } catch (Exception e) {
        throw new RingoException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
      }
      Authentication authentication = customAuthenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(info.email(), info.password(),
              new ArrayList<>())
      );
      /**
       * 로그인 할 때에는 authentication에 userDetail이 들어가고
       * stomp interceptor나 filter에서 jwt 인증을 할 때에는 User가 들어간다.
       */
      SecurityContextHolder.getContext().setAuthentication(authentication);
    }
    else if (isContains(request.getRequestURI())){
      SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new User(), "password",
          List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }
    else{
      log.info(request.getRequestURI());
    }
    filterChain.doFilter(request, response);
  }

  public boolean isContains(String url){
    for (String startUrl : whiteList){
      if (url.startsWith(startUrl)){
        return true;
      }
    }
    return false;
  }
}
