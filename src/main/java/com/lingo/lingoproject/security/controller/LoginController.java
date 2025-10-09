package com.lingo.lingoproject.security.controller;

import com.lingo.lingoproject.security.response.LoginResponseDto;
import com.lingo.lingoproject.security.services.LoginService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/login")
@RequiredArgsConstructor
public class LoginController {

  private final LoginService loginService;

  @PostMapping
  public ResponseEntity<?> login(HttpServletRequest request){
    LoginInfoDto info = null;
    try {
      info = (LoginInfoDto) request.getAttribute("requestBody");
    }catch (Exception e){
      e.printStackTrace();
    }
    LoginResponseDto response = loginService.login(info);
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
}
