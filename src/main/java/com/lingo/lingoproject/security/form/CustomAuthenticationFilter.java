package com.lingo.lingoproject.security.form;


import com.lingo.lingoproject.domain.UserEntity;
import com.lingo.lingoproject.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@WebFilter("/login")
@Order(1)
@RequiredArgsConstructor
@Component
public class CustomAuthenticationFilter extends OncePerRequestFilter {

  private final AuthenticationProvider authenticationProvider;
  private final UserRepository userRepository;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    if(request.getRequestURI().equals("/login") && request.getMethod().equalsIgnoreCase("POST")){
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
        Optional<UserEntity> user = userRepository.findByEmail(jsonObject.get("email").toString());
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.get().getRole().toString()));

        Authentication authentication = authenticationProvider.authenticate(
            new UsernamePasswordAuthenticationToken(jsonObject.get("email").toString(), "password", authorities)
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
