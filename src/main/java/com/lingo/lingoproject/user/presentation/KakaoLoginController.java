package com.lingo.lingoproject.user.presentation;
import com.lingo.lingoproject.user.application.KakaoLoginUseCase;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.security.TokenType;
import com.lingo.lingoproject.shared.security.jwt.JwtUtil;
import com.lingo.lingoproject.shared.security.dto.LoginResponseDto;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
@Hidden
public class KakaoLoginController {

  private final KakaoLoginUseCase kakaoLoginService;
  private final JwtUtil jwtUtil;
  private final RedisTemplate<String, Object> redisTemplate;


  @GetMapping("/kakao/callback")
  public ResponseEntity<?> callback(@RequestParam String code){
    log.info("step=카카오_로그인_콜백_시작");
    User user = kakaoLoginService.saveUserLoginInfo(code);

    String accessToken = jwtUtil.generateToken(TokenType.ACCESS, user);
    String refreshToken = jwtUtil.generateToken(TokenType.REFRESH, user);

    redisTemplate.opsForValue().set("redis::refresh::" + user.getLoginId(), refreshToken, 30, TimeUnit.DAYS);

    log.info("step=카카오_로그인_콜백_완료, userId={}", user.getId());

    return ResponseEntity.ok(new LoginResponseDto(ErrorCode.SUCCESS.getCode(), user.getId(), accessToken, refreshToken));
  }
}
