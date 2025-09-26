package com.lingo.lingoproject.security.oauth.google;

import com.lingo.lingoproject.domain.UserEntity;
import com.lingo.lingoproject.security.TokenType;
import com.lingo.lingoproject.security.jwt.JwtUtil;
import com.lingo.lingoproject.security.response.LoginResponseDto;
import com.lingo.lingoproject.security.util.RandomUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GoogleLoginController {

  private final GoogleLoginService googleLoginService;
  private final JwtUtil jwtUtil;
  private final RandomUtil randomUtil;

  @GetMapping("/google/callback")
  public ResponseEntity<?> callback(@RequestParam String code){

    UserEntity user = googleLoginService.saveUserLoginInfo(code);
    int rand = randomUtil.getRandomNumber();
    String accessToken = jwtUtil.generateToken(TokenType.ACCESS, user.getEmail(), rand);
    String refreshToken = jwtUtil.generateToken(TokenType.REFRESH, user.getEmail(), rand);
    return ResponseEntity.ok(new LoginResponseDto(user.getId(), accessToken, refreshToken));
  }

}
