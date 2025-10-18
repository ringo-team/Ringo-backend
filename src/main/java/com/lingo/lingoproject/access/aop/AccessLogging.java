package com.lingo.lingoproject.access.aop;

import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.repository.UserRepository;
import com.lingo.lingoproject.security.jwt.JwtUtil;
import com.lingo.lingoproject.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AccessLogging {

  private final UserService userService;
  private final JwtUtil jwtUtil;
  private final UserRepository userRepository;

  @Before("@annotation(com.lingo.lingoproject.access.annotation.AccessLog)")
  public void logBeforeAccess(JoinPoint joinPoint){
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    java.lang.Object principal =  authentication.getPrincipal();
    if(principal instanceof User user) {
      log.info(user.getNickname() + "이(가) 접속했습니다.");
      userService.saveUserAccessLog(user);
    }else{
      Object[] args = joinPoint.getArgs();
      String token = (String) args[0];
      try{
        User user = userRepository.findByEmail(jwtUtil.getClaims(token).getSubject()).orElseThrow();
        userService.saveUserAccessLog(user);
      }catch (Exception e){
        throw new RingoException("유효하지 않은 토큰입니다.", HttpStatus.FORBIDDEN);
      }
    }
  }
}
