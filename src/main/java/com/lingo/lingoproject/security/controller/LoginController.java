package com.lingo.lingoproject.security.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingo.lingoproject.security.jwt.JwtUtil;
import com.lingo.lingoproject.security.response.LoginResponseDto;
import com.lingo.lingoproject.security.services.LoginService;
import com.lingo.lingoproject.utils.RequestCacheWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class LoginController {

  private final LoginService loginService;
  private final RequestCacheWrapper  requestCache;
  private final JwtUtil jwtUtil;

  @PostMapping("/login")
  public ResponseEntity<?> login(){
    ObjectMapper objectMapper = new ObjectMapper();
    LoginInfoDto request = null;
    try {
      request = objectMapper.readValue(requestCache.toString(),LoginInfoDto.class);
    }catch (Exception e){
      e.printStackTrace();
    }
    LoginResponseDto response = loginService.login(request);
    return ResponseEntity.status(HttpStatus.OK)
        .body(response);
  }

  @GetMapping("/refresh")
  public ResponseEntity<?> refresh(@RequestParam String refreshToken){
    LoginResponseDto response = null;
    try{
      response = loginService.regenerateToken(refreshToken);
    }catch(NotFoundException e){
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }catch(Exception e){
      e.printStackTrace();
      return  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
    return ResponseEntity.status(HttpStatus.OK)
        .body(response);
  }

  @PostMapping("/signup")
  public ResponseEntity<?> signup(@RequestBody LoginInfoDto dto){
    log.info(dto.toString());
    loginService.signup(dto);
    return ResponseEntity.status(HttpStatus.OK).build();
  }

  @PostMapping("/logout")
  public ResponseEntity<?> logout(@RequestHeader(value = "token") String token){
    String accessToken = token.substring(7);
    loginService.logout(accessToken);
    return ResponseEntity.status(HttpStatus.OK).build();
  }
}
