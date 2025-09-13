package com.lingo.lingoproject.security.services;

import com.lingo.lingoproject.domain.JwtToken;
import com.lingo.lingoproject.domain.UserEntity;
import com.lingo.lingoproject.repository.JwtTokenRepository;
import com.lingo.lingoproject.repository.UserRepository;
import com.lingo.lingoproject.security.TokenType;
import com.lingo.lingoproject.security.controller.LoginInfoDto;
import com.lingo.lingoproject.security.response.LoginResponseDto;
import com.lingo.lingoproject.security.util.JwtUtil;
import com.lingo.lingoproject.security.util.RandomUtil;
import io.jsonwebtoken.Claims;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginService {

  private final JwtUtil jwtUtil;
  private final RandomUtil randomUtil;
  private final UserRepository userRepository;
  private final JwtTokenRepository jwtTokenRepository;

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

  public LoginResponseDto regenerateToken(String refreshToken) throws NotFoundException {
    Claims claims =  jwtUtil.getClaims(refreshToken);
    Optional<UserEntity> user = userRepository.findById((UUID) claims.get("userId"));
    if (user.isEmpty()){
      throw new NotFoundException();
    }
    JwtToken token = jwtTokenRepository.findByUser(user.get());
    if(token.getRefreshToken().equals(refreshToken)){
      int rand = randomUtil.getRandomNumber();
      String accessToken = jwtUtil.generateToken(TokenType.ACCESS, claims.getSubject(), rand);
      String refresh = jwtUtil.generateToken(TokenType.REFRESH, claims.getSubject(), rand);
      return new  LoginResponseDto(accessToken, refresh);
    }
    else{
      throw new NotFoundException();
    }
  }

}
