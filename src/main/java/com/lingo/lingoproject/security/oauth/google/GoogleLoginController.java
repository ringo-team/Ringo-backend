package com.lingo.lingoproject.security.oauth.google;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GoogleLoginController {

  private final GoogleLoginService googleLoginService;

  @GetMapping("/google/callback")
  public void callback(@RequestParam String code){
    googleLoginService.saveUserLoginInfo(code);
  }

}
