package com.lingo.lingoproject.security.oauth.google;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GoogleLoginController {
  @GetMapping("/google/callback")
  public void callback(@RequestParam String code){

  }
}
