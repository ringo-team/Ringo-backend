package com.lingo.lingoproject.security.oauth.google;

import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.security.TokenType;
import com.lingo.lingoproject.security.jwt.JwtUtil;
import com.lingo.lingoproject.security.dto.LoginResponseDto;
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

  @GetMapping("/google/callback")
  public ResponseEntity<?> callback(@RequestParam String code){

    User user = googleLoginService.saveUserLoginInfo(code);
    String accessToken = jwtUtil.generateToken(TokenType.ACCESS, user);
    String refreshToken = jwtUtil.generateToken(TokenType.REFRESH, user);
    jwtUtil.saveRefreshToken(refreshToken, user);
    return ResponseEntity.ok(new LoginResponseDto(user.getId(), accessToken, refreshToken));
  }

}
