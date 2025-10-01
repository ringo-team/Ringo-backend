package com.lingo.lingoproject.auth;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SelfAuthController {

  @GetMapping("self-auth/callback")
  public String selfAuthCallback(){
    
  }
}
