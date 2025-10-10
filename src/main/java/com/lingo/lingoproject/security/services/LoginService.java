package com.lingo.lingoproject.security.services;

import com.lingo.lingoproject.domain.JwtRefreshToken;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.repository.JwtRefreshTokenRepository;
import com.lingo.lingoproject.repository.UserRepository;
import com.lingo.lingoproject.security.TokenType;
import com.lingo.lingoproject.security.controller.LoginInfoDto;
import com.lingo.lingoproject.security.response.LoginResponseDto;
import com.lingo.lingoproject.security.jwt.JwtUtil;
import com.lingo.lingoproject.security.util.RandomUtil;
import com.lingo.lingoproject.utils.RedisUtils;
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
  private final JwtRefreshTokenRepository jwtRefreshTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final RedisUtils redisUtils;

  public LoginResponseDto login(LoginInfoDto dto){
    if (SecurityContextHolder.getContext().getAuthentication() == null) {
      throw new IllegalArgumentException("Invalid login info.");
    }
    /**
     * 로그인 진행시 access token과 refresh token을 발급한다.
     */
    String access = jwtUtil.generateToken(TokenType.ACCESS, dto.email());
    String refresh = jwtUtil.generateToken(TokenType.REFRESH, dto.email());
    User user = userRepository.findByEmail(dto.email())
        .orElseThrow(() -> new IllegalArgumentException("Invalid username or password."));

    /**
     * 재로그인의 경우에 이미 존재하는 jwtRefreshToken을 참고한다.
     */
    JwtRefreshToken tokenInfo = jwtRefreshTokenRepository.findByUser(user);
    if(tokenInfo != null){
      tokenInfo.setRefreshToken(refresh);
    }
    else {
      tokenInfo = JwtRefreshToken.builder()
          .refreshToken(refresh)
          .user(user)
          .build();
    }
    jwtRefreshTokenRepository.save(tokenInfo);
    return new  LoginResponseDto(user.getId(), access, refresh);
  }

  public LoginResponseDto regenerateToken(String refreshToken) throws NotFoundException {
    Claims claims =  jwtUtil.getClaims(refreshToken);
    Optional<User> user = userRepository.findById(Long.parseLong(claims.get("userId").toString()));
    if (user.isEmpty()){
      throw new NotFoundException();
    }
    User userEntity = user.get();
    JwtRefreshToken tokenInfo = jwtRefreshTokenRepository.findByUser(userEntity);
    if(tokenInfo != null && tokenInfo.getRefreshToken().equals(refreshToken) && jwtUtil.isValidToken(refreshToken)){
      String accessToken = jwtUtil.generateToken(TokenType.ACCESS, userEntity.getEmail());
      String refresh = jwtUtil.generateToken(TokenType.REFRESH, userEntity.getEmail());
      tokenInfo.setRefreshToken(refresh);
      jwtRefreshTokenRepository.save(tokenInfo);
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

  public void logout(String accessToken){
    Claims claims = jwtUtil.getClaims(accessToken);
    Long userId = (Long) claims.get("userId");
    User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다."));
    // redis의 blacklist에 저장해 놓는다.
    redisUtils.saveBlackList(accessToken, "true");

    /**
     * 로그아웃 시 refresh 토큰에 관한 정보도 삭제하여 토큰 재발급을 막는다.
     */
    jwtRefreshTokenRepository.deleteAllByUser(user);

  }

}
