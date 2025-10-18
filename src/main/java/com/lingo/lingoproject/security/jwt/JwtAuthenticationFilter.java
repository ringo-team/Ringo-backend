package com.lingo.lingoproject.security.jwt;


import com.amazonaws.services.kms.model.NotFoundException;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.repository.UserRepository;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@WebFilter
@Order(3)
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final JwtUtil jwtUtil;
  private final RedisUtils redisUtils;
  private final UserRepository userRepository;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    String accessToken = request.getHeader("Authorization");
    if (accessToken != null && accessToken.startsWith("Bearer ")) {
      accessToken = accessToken.substring(7);

      /**
       * blackList에 들어있는 토큰인지 확인
       * blackList는 로그아웃한 유저들의 토큰을 모아놓은 리스트임
       * redis에 blackList가 저장되어 있음. 유효기간은 1일임
       * 유저가 다시 로그인을 하면 새로 accessToken을 발급받기 때문에 삭제할 필요없음
       */
      if(redisUtils.containsBlackList(accessToken)){
        throw new RingoException("유효하지 않은 토큰 입니다.", HttpStatus.FORBIDDEN);
      }
      /**
       * 유효한 토큰인지 확인
       * (비밀키에 제대로 파싱이 되는지)
       * 유효기간이 지난 토큰일 경우 예외를 발생시킴
       */
      else if (!jwtUtil.isValidToken(accessToken)) {
        throw new RingoException("유효하지 않은 토큰 입니다.", HttpStatus.FORBIDDEN);
      }

      Claims claims = jwtUtil.getClaims(accessToken);
      User user = userRepository.findByEmail(claims.getSubject())
          .orElseThrow(() -> new RingoException("유효하지 않은 토큰입니다.", HttpStatus.FORBIDDEN));
      SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken(user, "password", user.getAuthorities())
      );
    }else{
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if(!(authentication.getPrincipal() instanceof User)) {
        throw new RingoException("인증되지 않았습니다.", HttpStatus.FORBIDDEN);
      }
    }
    filterChain.doFilter(request, response);
  }
}
