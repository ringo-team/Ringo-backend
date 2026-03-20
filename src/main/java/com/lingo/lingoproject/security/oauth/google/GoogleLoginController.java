package com.lingo.lingoproject.security.oauth.google;

import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.exception.ErrorCode;
import com.lingo.lingoproject.security.TokenType;
import com.lingo.lingoproject.security.controller.dto.SignupResponseDto;
import com.lingo.lingoproject.security.jwt.JwtUtil;
import com.lingo.lingoproject.security.dto.LoginResponseDto;
import com.lingo.lingoproject.exception.RingoException;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
@Hidden
public class GoogleLoginController {

  private final GoogleLoginService googleLoginService;
  private final JwtUtil jwtUtil;
  private final RedisTemplate<String, Object> redisTemplate;

  @GetMapping("/google/callback")
  public ResponseEntity<?> callback(@RequestParam String code){
    log.info("step=구글_로그인_콜백_시작, status=SUCCESS");

    User user = googleLoginService.saveUserLoginInfo(code);


    String accessToken = jwtUtil.generateToken(TokenType.ACCESS, user);
    String refreshToken = jwtUtil.generateToken(TokenType.REFRESH, user);

    redisTemplate.opsForValue().set("redis::refresh::" + user.getLoginId(), refreshToken, 30, TimeUnit.DAYS);

    log.info("userId={}, step=구글_로그인_콜백_완료, status=SUCCESS", user.getId());
    return ResponseEntity.ok(new LoginResponseDto(ErrorCode.SUCCESS.getCode(), user.getId(), accessToken, refreshToken));
  }

}
