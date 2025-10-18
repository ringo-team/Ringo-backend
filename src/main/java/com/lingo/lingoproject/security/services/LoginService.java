package com.lingo.lingoproject.security.services;

import com.lingo.lingoproject.domain.JwtRefreshToken;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.domain.enums.Role;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.repository.JwtRefreshTokenRepository;
import com.lingo.lingoproject.repository.UserRepository;
import com.lingo.lingoproject.security.TokenType;
import com.lingo.lingoproject.security.controller.LoginInfoDto;
import com.lingo.lingoproject.security.dto.LoginResponseDto;
import com.lingo.lingoproject.security.jwt.JwtUtil;
import com.lingo.lingoproject.utils.RedisUtils;
import io.jsonwebtoken.Claims;
import jakarta.transaction.Transactional;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginService {

  private final JwtUtil jwtUtil;
  private final UserRepository userRepository;
  private final JwtRefreshTokenRepository jwtRefreshTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final RedisUtils redisUtils;

  public LoginResponseDto login(LoginInfoDto dto){
    if (SecurityContextHolder.getContext().getAuthentication() == null) {
      throw new RingoException("유저가 인증되지 않았습니다.", HttpStatus.FORBIDDEN);
    }

    /**
     * contextHolder에 Authentication 객체가 존재한다는 것은 이미 로그인에 성공했다는 의미이므로
     * 데이터베이스에 이메일에 해당하는 user가 존재한다는 것을 보장할 수 있다. 따라서 get으로 optional을 캡슐을 제거한다.
     */
    User user = userRepository.findByEmail(dto.email()).get();

    /**
     * 로그인 진행시 access token과 refresh token을 발급한다.
     */
    String access = jwtUtil.generateToken(TokenType.ACCESS, user);
    String refresh = jwtUtil.generateToken(TokenType.REFRESH, user);

    /**
     * 로그아웃을 하지 않고 리프레시 토큰이 유효기간이 지날 때까지
     * 접속을 안하면 재로그인을 해야한다. 이때 리프레시 토큰은 db에 저장되어 있다.
     * 로그인 성공 시 저장된 리프레시 토큰이 업데이트 된다.
     */
    JwtRefreshToken tokenInfo = jwtRefreshTokenRepository.findByUser(user)
        .orElse(null);
    if(tokenInfo != null){
      tokenInfo.setRefreshToken(refresh);
    }
    else {
      /**
       * 로그아웃을 했거나 신규가입자의 경우 리프레시 토큰이 존재하지 않으므로
       */
      tokenInfo = JwtRefreshToken.builder()
          .refreshToken(refresh)
          .user(user)
          .build();
    }
    jwtRefreshTokenRepository.save(tokenInfo);
    return new  LoginResponseDto(user.getId(), access, refresh);
  }

  /**
   * 서버에 저장된 refresh token과 비교, 토큰의 유효성을 검증한다.
   * accessToken과 refreshToken을 각각 재발행한 후에
   * db에 저장되어 있던 refreshToken을 업데이트한다.
   * @param refreshToken
   * @return
   */
  public LoginResponseDto regenerateToken(String refreshToken){
    Claims claims =  jwtUtil.getClaims(refreshToken);
    Optional<User> optionalUser = userRepository.findByEmail(claims.getSubject());
    if (optionalUser.isEmpty()){
      throw new RingoException("유효하지 않은 토큰입니다.",  HttpStatus.FORBIDDEN);
    }
    User user = optionalUser.get();
    JwtRefreshToken tokenInfo = jwtRefreshTokenRepository.findByUser(user)
        .orElseThrow(() -> new RingoException("토큰을 찾을 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR));
    /**
     * 저장된 리프레시 토큰과 동일해야 하며
     * 리프레시 토큰의 유효기간이 지나지 않아야한다.
     */
    if(tokenInfo.getRefreshToken().equals(refreshToken) && jwtUtil.isValidToken(refreshToken)){
      String accessToken = jwtUtil.generateToken(TokenType.ACCESS, user);
      String refresh = jwtUtil.generateToken(TokenType.REFRESH, user);
      // 리프레시 토큰의 값을 업데이트 한다.
      tokenInfo.setRefreshToken(refresh);
      jwtRefreshTokenRepository.save(tokenInfo);
      return new  LoginResponseDto(user.getId(), accessToken, refresh);
    }
    else throw new RingoException("유효하지 않은 토큰입니다.", HttpStatus.FORBIDDEN);
  }

  public void signup(LoginInfoDto dto){
    User user = User.builder()
        .email(dto.email())
        .password(passwordEncoder.encode(dto.password()))
        .role(Role.USER)
        .build();
    userRepository.save(user);
  }

  @Transactional
  public void logout(User user, String accessToken){
    // redis의 blacklist에 저장해 놓는다.
    redisUtils.saveBlackList(accessToken.substring(7), "true");

    /**
     * 로그아웃 시 refresh 토큰에 관한 정보도 삭제하여 토큰 재발급을 막는다.
     */
    jwtRefreshTokenRepository.deleteByUser(user);

  }

}
