package com.lingo.lingoproject.security.form;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@WebFilter("/login")
@Order(1)
@RequiredArgsConstructor
public class CustomAuthenticationFilter extends OncePerRequestFilter {

  private final AuthenticationProvider authenticationProvider;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    if(request.getRequestURI().equals("/login") || request.getMethod().equalsIgnoreCase("POST")){
      String requestBody = request.getReader()
          .lines()
          .collect(Collectors.joining(System.lineSeparator()));
      JSONParser  jsonParser = new JSONParser();
      JSONObject jsonObject = null;
      try {
        jsonObject = (JSONObject) jsonParser.parse(requestBody);
      } catch (ParseException e) {
        throw new RuntimeException(e);
      }
      String method = jsonObject.get("method").toString();
      if (method.equals("form")){
        Authentication authentication = authenticationProvider.authenticate(
            new UsernamePasswordAuthenticationToken(jsonObject.get("email").toString(), "password")
        );
        if(authentication.isAuthenticated()){
          SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        else{
          throw new BadCredentialsException("Bad credentials");
        }
      }
    }
    filterChain.doFilter(request, response);
  }
}
