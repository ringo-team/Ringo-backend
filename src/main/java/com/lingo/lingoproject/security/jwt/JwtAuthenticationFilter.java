package com.lingo.lingoproject.security.jwt;


import com.lingo.lingoproject.utils.RedisUtils;
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
  private final RedisUtils redisUtils;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    String accessToken = request.getHeader("Authorization");
    if (accessToken != null && accessToken.startsWith("Bearer ")) {
      accessToken = accessToken.substring(7);

      /**
       * blackList에 들어있는 토큰인지 확인
       * blackList는 로그아웃한 유저들의 토큰을 모아놓은 리스트임
       * redis에 blackList가 저장되어 있음. 유효기간은 2일임
       */
      if(redisUtils.containsBlackList(accessToken)){
        response.setStatus(HttpStatus.FORBIDDEN.value());
      }
      /**
       * 유효한 토큰인지 확인
       * (비밀키에 제대로 파싱이 되는지)
       */
      else if (jwtUtil.isValidToken(accessToken)) {
        Claims claims = jwtUtil.getClaims(accessToken);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(claims.getSubject(), "password")
        );
      }
      /**
       * 유효시간이 지난 토큰은 refresh token 재발급 요청
       */
      else if (jwtUtil.isExpiredToken(accessToken)){
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
      }
      /**
       * 유효한 토큰도 아니고 유효기간이 지난 토큰도 아닌 경우 접근을 금지함
       */
      else{
        response.setStatus(HttpStatus.FORBIDDEN.value());
      }
    }
    filterChain.doFilter(request, response);
  }
}
