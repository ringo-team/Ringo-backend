package com.lingo.lingoproject.security.oauth.kakao;

import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.exception.ErrorCode;
import com.lingo.lingoproject.security.TokenType;
import com.lingo.lingoproject.security.jwt.JwtUtil;
import com.lingo.lingoproject.security.dto.LoginResponseDto;
import com.lingo.lingoproject.exception.RingoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class KakaoLoginController {

  private final KakaoLoginService kakaoLoginService;
  private final JwtUtil jwtUtil;


  @GetMapping("/kakao/callback")
  public ResponseEntity<?> callback(@RequestParam String code){
    try {

      log.info("step=카카오_로그인_콜백_시작, status=SUCCESS");
      User user = kakaoLoginService.saveUserLoginInfo(code);
      String accessToken = jwtUtil.generateToken(TokenType.ACCESS, user);
      String refreshToken = jwtUtil.generateToken(TokenType.REFRESH, user);
      jwtUtil.saveRefreshToken(refreshToken, user);
      log.info("userId={}, step=카카오_로그인_콜백_완료, status=SUCCESS", user.getId());

      return ResponseEntity.ok(new LoginResponseDto(ErrorCode.SUCCESS.getCode(), user.getId(), accessToken, refreshToken));
    } catch (Exception e) {
      log.error("step=카카오_로그인_콜백_실패, status=FAILED", e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("카카오 로그인 처리에 실패했습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
