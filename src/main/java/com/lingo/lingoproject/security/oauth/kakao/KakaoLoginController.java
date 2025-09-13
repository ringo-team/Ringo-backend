package com.lingo.lingoproject.security.oauth.kakao;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class KakaoLoginController {

  private final KakaoLoginService kakaoLoginService;

  @GetMapping("/kakao/callback")
  public void callback(@RequestParam String code){
    kakaoLoginService.saveUserLoginInfo(code);

  }
}
