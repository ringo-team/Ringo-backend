package com.lingo.lingoproject.security.controller;

import com.lingo.lingoproject.security.response.LoginResponseDto;
import com.lingo.lingoproject.security.services.LoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/login")
@RequiredArgsConstructor
public class LoginController {

  private final LoginService loginService;

  @PostMapping
  public ResponseEntity<?> login(LoginInfoDto dto){
    LoginResponseDto response = loginService.login(dto);
    return ResponseEntity.status(HttpStatus.OK)
        .body(response);
  }
}
