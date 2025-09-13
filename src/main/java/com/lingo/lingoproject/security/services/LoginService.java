package com.lingo.lingoproject.security.services;

import com.lingo.lingoproject.security.TokenType;
import com.lingo.lingoproject.security.controller.LoginInfoDto;
import com.lingo.lingoproject.security.response.LoginResponseDto;
import com.lingo.lingoproject.security.util.JwtUtil;
import com.lingo.lingoproject.security.util.RandomUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginService {

  private final JwtUtil jwtUtil;
  private final RandomUtil randomUtil;

  public LoginResponseDto login(LoginInfoDto dto){
    if(dto.method().equals("google")){

    }
    else if(dto.method().equals("apple")){

    }
    else if(dto.method().equals("kakao")){

    }
    else{

    }
    if (SecurityContextHolder.getContext().getAuthentication() == null) {
      throw new IllegalArgumentException("Invalid login info.");
    }
    int rand = randomUtil.getRandomNumber();
    String access = jwtUtil.generateToken(TokenType.ACCESS, dto.username(), rand);
    String refresh = jwtUtil.generateToken(TokenType.REFRESH, dto.username(), rand);
    return new  LoginResponseDto(access, refresh);
  }

}
