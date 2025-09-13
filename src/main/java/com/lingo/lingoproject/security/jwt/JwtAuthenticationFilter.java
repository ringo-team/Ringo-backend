package com.lingo.lingoproject.security.jwt;


import com.lingo.lingoproject.security.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@WebFilter
@Order(2)
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final JwtUtil jwtUtil;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    String accessToken = request.getHeader("Authorization");
    if (accessToken != null && accessToken.startsWith("Bearer ")) {
      accessToken = accessToken.substring(7);
      if (jwtUtil.validateToken(accessToken)) {
        Claims claims = jwtUtil.getClaims(accessToken);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(claims.getSubject(), "password")
        );
      }
      /*
        유효시간이 지난 토큰은 refresh token 재발급 요청
       */
      else if (jwtUtil.isAuthenticatedToken(accessToken)){
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
      }
    }
    filterChain.doFilter(request, response);
  }
}
