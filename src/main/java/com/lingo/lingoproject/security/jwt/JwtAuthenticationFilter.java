package com.lingo.lingoproject.security.jwt;


import com.lingo.lingoproject.domain.BlockedUser;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.domain.enums.SignupStatus;
import com.lingo.lingoproject.exception.ErrorCode;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.repository.BlockedUserRepository;
import com.lingo.lingoproject.repository.UserRepository;
import com.lingo.lingoproject.utils.RedisUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final JwtUtil jwtUtil;
  private final RedisUtils redisUtils;
  private final UserRepository userRepository;
  private final BlockedUserRepository blockedUserRepository;


  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    String accessToken = request.getHeader("Authorization");
    if (accessToken != null && accessToken.startsWith("Bearer ")) {
      accessToken = accessToken.substring(7);

      Claims claims = jwtUtil.getClaims(accessToken);
      User user = userRepository.findByEmail(claims.getSubject())
          .orElseThrow(() -> new RingoException("유효하지 않은 토큰입니다.", ErrorCode.TOKEN_INVALID, HttpStatus.FORBIDDEN));

      // 로그아웃한 유저가 기존 토큰으로 접근하려고 할때 접근을 차단함
      if(redisUtils.containsLogoutUserList(accessToken)){
        throw new RingoException("유효하지 않은 토큰 입니다.", ErrorCode.LOGOUT, HttpStatus.FORBIDDEN);
      }

      // 계정 정지된 사람일 경우 접근을 차단함
      if (redisUtils.isSuspendedUser(user.getId())){
        throw new RingoException("계정이 정지된 유저입니다.", ErrorCode.BLOCKED, HttpStatus.FORBIDDEN);
      }

      // 영구 정지된 사람인 경우 접근을 차단함
      List<Long> blockUserIds = blockedUserRepository.findAll()
          .stream()
          .map(BlockedUser::getId)
          .toList();
      if (blockUserIds.contains(user.getId())) {
        throw new RingoException("영구정지된 유저입니다.", ErrorCode.BLOCKED, HttpStatus.FORBIDDEN);
      }

      // 회원가입을 마치치 않은 회원의 경우 접근을 차단함
      if(!(request.getRequestURI().startsWith("/signup") ||
          request.getRequestURI().equals("/profiles") ||
          user.getStatus().equals(SignupStatus.COMPLETED))){
        throw new RingoException("회원가입을 마치고 요청 주시길 바랍니다.", ErrorCode.BEFORE_SIGNUP ,HttpStatus.FORBIDDEN);
      }

      log.info("userId={}, endpoint={}, step=api_요청", user.getId(), request.getRequestURI());

      SecurityContextHolder.getContext().setAuthentication(
          new UsernamePasswordAuthenticationToken(user, "password", user.getAuthorities())
      );
    }

    filterChain.doFilter(request, response);
  }
}
