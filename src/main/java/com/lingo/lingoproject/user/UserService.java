package com.lingo.lingoproject.user;

import com.lingo.lingoproject.domain.BlockedUser;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.domain.enums.Religion;
import com.lingo.lingoproject.repository.BlockedUserRepository;
import com.lingo.lingoproject.repository.UserRepository;
import com.lingo.lingoproject.security.jwt.JwtUtil;
import com.lingo.lingoproject.user.dto.GetUserInfoResponseDto;
import com.lingo.lingoproject.user.dto.UpdateUserInfoRequestDto;
import com.lingo.lingoproject.utils.RedisUtils;
import io.jsonwebtoken.Claims;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
  private final UserRepository userRepository;
  private final JwtUtil jwtUtil;
  private final RedisUtils redisUtils;
  private final BlockedUserRepository blockedUserRepository;

  public void deleteUser(Long userId, String token){
    Long id = getUserIdByToken(token);
    if(!Objects.equals(userId, id)){
      throw new IllegalArgumentException("잘못된 요청입니다.");
    }
    userRepository.deleteById(userId);
  }

  public void adminDeleteUser(Long userId){
    userRepository.deleteById(userId);
  }

  public String findUserId(String token) throws Exception{
    Long userId = getUserIdByToken(token);
    boolean isAuthenticated = (boolean) redisUtils.getDecryptKeyObject("self-auth" + userId);
    if(isAuthenticated){
      User user = userRepository.findById(userId)
          .orElseThrow(() -> new IllegalArgumentException("회원이 아닙니다."));
      return user.getUsername();
    }
    else{
      throw new Exception("인증되지 않았습니다.");
    }
  }

  public void resetPassword(String password, String token) throws Exception{
    Long userId = getUserIdByToken(token);
    boolean isAuthenticated = (boolean) redisUtils.getDecryptKeyObject("self-auth" + userId);
    if(isAuthenticated){
      User user = userRepository.findById(userId)
          .orElseThrow(() -> new IllegalArgumentException("회원이 아닙니다."));
      user.setPassword(password);
      userRepository.save(user);
    }
    else{
      throw new Exception("인증되지 않았습니다.");
    }
  }

  public GetUserInfoResponseDto getUserInfo(String token){
    Long userId = getUserIdByToken(token);
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("id에 해당하는 유저가 없습니다."));
    return GetUserInfoResponseDto.builder()
        .id(user.getId())
        .gender(user.getGender().toString())
        .email(user.getEmail())
        .height(user.getHeight())
        .isDrinking(user.getIsDrinking())
        .isSmoking(user.getIsSmoking())
        .religion(user.getReligion().toString())
        .job(user.getJob())
        .nickname(user.getNickname())
        .build();
  }

  public void updateUserInfo(String token, UpdateUserInfoRequestDto dto){
    Long userId = getUserIdByToken(token);
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("id에 해당하는 유저가 없습니다."));
    if (dto.height() != null) user.setHeight(dto.height());
    if (dto.isDrinking() != null) user.setIsDrinking(dto.isDrinking());
    if (dto.isSmoking() != null) user.setIsSmoking(dto.isSmoking());
    if (dto.religion() != null) user.setReligion(Religion.valueOf(dto.religion()));
    if (dto.job() != null) user.setJob(dto.job());
    if (dto.etc() != null) user.setEtc(dto.etc());

    userRepository.save(user);
  }

  public Long getUserIdByToken(String token){
    String accessToken = token.substring(7);
    Claims claims = jwtUtil.getClaims(accessToken);
    return Long.valueOf(claims.get("userId").toString());
  }

  public List<GetUserInfoResponseDto> getPageableUserInfo(int page, int size){
    Pageable pageable = PageRequest.of(page, size);
    Page<User> users = userRepository.findAll(pageable);
    return users.stream()
        .map(user -> {
            return GetUserInfoResponseDto.builder()
              .id(user.getId())
              .gender(user.getGender().toString())
              .email(user.getEmail())
              .height(user.getHeight())
              .isDrinking(user.getIsDrinking())
              .isSmoking(user.getIsSmoking())
              .religion(user.getReligion().toString())
              .job(user.getJob())
              .nickname(user.getNickname())
              .build();
        })
        .toList();
  }

  public void blockUser(Long userId, String token){
    Long adminId = getUserIdByToken(token);
    User admin = userRepository.findById(adminId)
        .orElseThrow(() -> new IllegalArgumentException("id 에 해당하는 관리자가 없습니다."));
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("id에 해당하는 유저가 없습니다."));
    BlockedUser blockedUser = BlockedUser.builder()
        .phoneNumber(user.getPhoneNumber())
        .admin(admin)
        .build();
    blockedUserRepository.save(blockedUser);
  }

}
