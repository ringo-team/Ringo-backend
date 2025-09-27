package com.lingo.lingoproject.security.services;

import com.lingo.lingoproject.domain.JwtRefreshToken;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.repository.JwtTokenRepository;
import com.lingo.lingoproject.repository.UserRepository;
import com.lingo.lingoproject.security.TokenType;
import com.lingo.lingoproject.security.controller.LoginInfoDto;
import com.lingo.lingoproject.security.response.LoginResponseDto;
import com.lingo.lingoproject.security.jwt.JwtUtil;
import com.lingo.lingoproject.security.util.RandomUtil;
import io.jsonwebtoken.Claims;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginService {

  private final JwtUtil jwtUtil;
  private final RandomUtil randomUtil;
  private final UserRepository userRepository;
  private final JwtTokenRepository jwtTokenRepository;
  private final PasswordEncoder passwordEncoder;

  public LoginResponseDto login(LoginInfoDto dto){
    if (SecurityContextHolder.getContext().getAuthentication() == null) {
      throw new IllegalArgumentException("Invalid login info.");
    }
    int rand = randomUtil.getRandomNumber();
    String access = jwtUtil.generateToken(TokenType.ACCESS, dto.email(), rand);
    String refresh = jwtUtil.generateToken(TokenType.REFRESH, dto.email(), rand);
    User user = userRepository.findByEmail(dto.email())
        .orElseThrow(() -> new IllegalArgumentException("Invalid username or password."));
    JwtRefreshToken refreshToken = JwtRefreshToken.builder()
        .refreshToken(refresh)
        .user(user)
        .rand(rand)
        .build();
    jwtTokenRepository.save(refreshToken);
    return new  LoginResponseDto(user.getId(), access, refresh);
  }

  public LoginResponseDto regenerateToken(String refreshToken) throws NotFoundException {
    Claims claims =  jwtUtil.getClaims(refreshToken);
    Optional<User> user = userRepository.findById(Long.parseLong(claims.get("userId").toString()));
    if (user.isEmpty()){
      throw new NotFoundException();
    }
    User userEntity = user.get();
    JwtRefreshToken token = jwtTokenRepository.findByUser(userEntity);
    if(token.getRefreshToken().equals(refreshToken) && jwtUtil.validateToken(token.getRefreshToken())){
      int rand = randomUtil.getRandomNumber();
      String accessToken = jwtUtil.generateToken(TokenType.ACCESS, userEntity.getEmail(), rand);
      String refresh = jwtUtil.generateToken(TokenType.REFRESH, userEntity.getEmail(), rand);
      return new  LoginResponseDto(userEntity.getId(), accessToken, refresh);
    }
    else{
      throw new NotFoundException();
    }
  }

  public void signup(LoginInfoDto dto){
    User user = User.builder()
        .email(dto.email())
        .password(passwordEncoder.encode(dto.password()))
        .build();
    userRepository.save(user);
  }

}
