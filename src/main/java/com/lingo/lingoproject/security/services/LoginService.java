package com.lingo.lingoproject.security.services;

import com.lingo.lingoproject.domain.BlockedUser;
import com.lingo.lingoproject.domain.FcmToken;
import com.lingo.lingoproject.domain.Hashtag;
import com.lingo.lingoproject.domain.JwtRefreshToken;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.domain.UserPoint;
import com.lingo.lingoproject.domain.enums.Drinking;
import com.lingo.lingoproject.domain.enums.Religion;
import com.lingo.lingoproject.domain.enums.Role;
import com.lingo.lingoproject.domain.enums.SignupStatus;
import com.lingo.lingoproject.domain.enums.Smoking;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.repository.BlockedUserRepository;
import com.lingo.lingoproject.repository.FcmTokenRepository;
import com.lingo.lingoproject.repository.HashtagRepository;
import com.lingo.lingoproject.repository.JwtRefreshTokenRepository;
import com.lingo.lingoproject.repository.UserPointRepository;
import com.lingo.lingoproject.repository.UserRepository;
import com.lingo.lingoproject.security.TokenType;
import com.lingo.lingoproject.security.controller.dto.LoginInfoDto;
import com.lingo.lingoproject.security.controller.dto.SignupUserInfoDto;
import com.lingo.lingoproject.security.dto.LoginResponseDto;
import com.lingo.lingoproject.security.dto.RegenerateTokenResponseDto;
import com.lingo.lingoproject.security.jwt.JwtUtil;
import com.lingo.lingoproject.utils.GenericUtils;
import com.lingo.lingoproject.utils.RedisUtils;
import io.jsonwebtoken.Claims;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginService {

  private final JwtUtil jwtUtil;
  private final UserRepository userRepository;
  private final JwtRefreshTokenRepository jwtRefreshTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final RedisUtils redisUtils;
  private final GenericUtils genericUtils;
  private final HashtagRepository hashtagRepository;
  private final BlockedUserRepository blockedUserRepository;
  private final UserPointRepository userPointRepository;
  private final FcmTokenRepository fcmTokenRepository;

  public LoginResponseDto login(LoginInfoDto dto){
    if (SecurityContextHolder.getContext().getAuthentication() == null) {
      throw new RingoException("유저가 인증되지 않았습니다.", HttpStatus.FORBIDDEN);
    }

    User user = userRepository.findByEmail(dto.email())
        .orElseThrow(() -> new RingoException("해당 이메일을 가진 유저를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST));

    // 토큰 재발급
    String access = jwtUtil.generateToken(TokenType.ACCESS, user);
    String refresh = jwtUtil.generateToken(TokenType.REFRESH, user);

    // 리프레시 토큰 업데이트
    JwtRefreshToken tokenInfo = jwtRefreshTokenRepository.findByUser(user)
        .orElseThrow(() -> new RingoException("유저의 리프레시 토큰을 찾을 수 없습니다.", HttpStatus.BAD_REQUEST));
    tokenInfo.setRefreshToken(refresh);
    jwtRefreshTokenRepository.save(tokenInfo);

    return new  LoginResponseDto(user.getId(), access, refresh);
  }

  /**
   * 서버에 저장된 refresh token과 비교하여 토큰의 유효성을 검증한다.
   * accessToken과 refreshToken을 각각 재발급한 후에
   * db에 저장되어 있는 refreshToken을 업데이트한다.
   */
  public RegenerateTokenResponseDto regenerateToken(String refreshToken){


    Claims claims =  jwtUtil.getClaims(refreshToken);

    User user = userRepository.findByEmail(claims.getSubject())
        .orElseThrow(() -> new RingoException("해당 이메일을 가진 유저를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST));

    JwtRefreshToken tokenInfo = jwtRefreshTokenRepository.findByUser(user)
        .orElseThrow(() -> new RingoException("토큰을 찾을 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR));

    if(!tokenInfo.getRefreshToken().equals(refreshToken)){
      throw new RingoException("유효하지 않은 토큰입니다.", HttpStatus.FORBIDDEN);
    }

    // 토큰 재발급
    String accessToken = jwtUtil.generateToken(TokenType.ACCESS, user);
    String refresh = jwtUtil.generateToken(TokenType.REFRESH, user);

    // 리프레시 토큰 업데이트
    tokenInfo.setRefreshToken(refresh);
    jwtRefreshTokenRepository.save(tokenInfo);

    return new  RegenerateTokenResponseDto(user.getId(), accessToken, refresh);
  }

  public User signup(LoginInfoDto dto){
    if(!dto.email().matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]+$")){
      throw new RingoException("적절하지 않은 입력값입니다.", HttpStatus.NOT_ACCEPTABLE);
    }
    if(!dto.password().matches("^(?=.*[A-Za-z])(?=.*\\d).+$")){
      throw new RingoException("적절하지 않은 입력값입니다.", HttpStatus.NOT_ACCEPTABLE);
    }
    if(userRepository.existsByEmail(dto.email())){
      throw new RingoException("중복된 이메일 입니다.", HttpStatus.NOT_ACCEPTABLE);
    }
    User user = User.builder()
        .email(dto.email())
        .password(passwordEncoder.encode(dto.password()))
        .role(Role.USER)
        .status(SignupStatus.BEFORE)
        .build();
    return  userRepository.save(user);
  }

  public boolean verifyDuplicatedLoginId(String email){
    return userRepository.existsByEmail(email);
  }

  @Transactional
  public void saveUserInfo(SignupUserInfoDto dto){
    if(!genericUtils.isContains(Smoking.values(), dto.isSmoking())){
      throw new RingoException("흡연 카테고리에 포함되지 않습니다.", HttpStatus.BAD_REQUEST);
    }
    if (!genericUtils.isContains(Drinking.values(), dto.isDrinking())){
      throw new RingoException("음주 카테고리에 포함되지 않습니다.", HttpStatus.BAD_REQUEST);
    }
    if (!genericUtils.isContains(Religion.values(), dto.religion())){
      throw new RingoException("종교 카테고리에 포함되지 않습니다.",  HttpStatus.BAD_REQUEST);
    }
    User user = userRepository.findById(dto.id()).orElseThrow(() -> new RingoException("해당 회원을 찾을 수 없습니다.", HttpStatus.BAD_REQUEST));
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(user.getBirthday());

    if(calendar.get(Calendar.YEAR) + 19 > LocalDate.now().getYear()){
      throw new RingoException("미성년자는 회원가입이 불가합니다.",  HttpStatus.FORBIDDEN);
    }

    if(user.getPhoneNumber() == null || user.getPhoneNumber().isEmpty()){
      throw new RingoException("본인인증 되지 않은 회원입니다.",  HttpStatus.BAD_REQUEST);
    }
    List<String> blockUserPhoneNumberList = blockedUserRepository.findAll()
        .stream()
        .map(BlockedUser::getPhoneNumber)
        .toList();
    if(blockUserPhoneNumberList.contains(user.getPhoneNumber().trim())){
      throw new RingoException("블락된 유저 입니다.", HttpStatus.FORBIDDEN);
    }
    user.setUserInfo(dto);
    user.setFriendInvitationCode(generateFriendInvitationCode());
    List<Hashtag> hashtags = new ArrayList<>();
    for (String tag : dto.hashtags()){
      hashtags.add(Hashtag.builder()
              .user(user)
              .hashtag(tag)
          .build());
    }
    try {
      userRepository.save(user);
      hashtagRepository.saveAll(hashtags);
      userPointRepository.save(UserPoint.builder().user(user).build());
      fcmTokenRepository.save(FcmToken.builder().user(user).build());
      jwtRefreshTokenRepository.save(JwtRefreshToken.builder().user(user).build());
    } catch (DataIntegrityViolationException e) {
      log.error("회원가입 데이터 무결성 위반. userId: {}", user.getId(), e);
      throw new RingoException(e.getMessage(), HttpStatus.BAD_REQUEST);
    } catch (Exception e) {
      log.error("회원가입 처리 중 예기치 못한 오류 발생. userId: {}", user.getId(), e);
      throw new RingoException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  public String generateFriendInvitationCode(){
    return Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes()).substring(0, 8);
  }


  @Transactional
  public void logout(User user, String accessToken){
    // redis의 blacklist에 저장해 놓는다.
    redisUtils.saveLogoutUserList(accessToken.substring(7), "true");

    /*
     * 로그아웃 시 refresh 토큰에 관한 정보도 삭제하여 토큰 재발급을 막는다.
     */
    jwtRefreshTokenRepository.deleteByUser(user);

  }

}
