package com.lingo.lingoproject.user;

import com.lingo.lingoproject.domain.BlockedUser;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.domain.UserAccessLog;
import com.lingo.lingoproject.domain.enums.Drinking;
import com.lingo.lingoproject.domain.enums.Religion;
import com.lingo.lingoproject.domain.enums.Smoking;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.image.ImageService;
import com.lingo.lingoproject.repository.AnsweredSurveyRepository;
import com.lingo.lingoproject.repository.BlockedFriendRepository;
import com.lingo.lingoproject.repository.BlockedUserRepository;
import com.lingo.lingoproject.repository.ChatroomParticipantRepository;
import com.lingo.lingoproject.repository.DormantAccountRepository;
import com.lingo.lingoproject.repository.JwtRefreshTokenRepository;
import com.lingo.lingoproject.repository.MatchingRepository;
import com.lingo.lingoproject.repository.MessageRepository;
import com.lingo.lingoproject.repository.UserAccessLogRepository;
import com.lingo.lingoproject.repository.UserRepository;
import com.lingo.lingoproject.user.dto.GetUserInfoResponseDto;
import com.lingo.lingoproject.user.dto.UpdateUserInfoRequestDto;
import com.lingo.lingoproject.utils.GenericUtils;
import com.lingo.lingoproject.utils.RedisUtils;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final RedisUtils redisUtils;
  private final BlockedUserRepository blockedUserRepository;
  private final AnsweredSurveyRepository answeredSurveyRepository;
  private final BlockedFriendRepository blockedFriendRepository;
  private final DormantAccountRepository dormantAccountRepository;
  private final ImageService imageService;
  private final MessageRepository messageRepository;
  private final MatchingRepository matchingRepository;
  private final JwtRefreshTokenRepository jwtRefreshTokenRepository;
  private final ChatroomParticipantRepository chatroomParticipantRepository;
  private final UserAccessLogRepository userAccessLogRepository;
  private final GenericUtils genericUtils;

  @Transactional
  public void deleteUser(Long userId){
    /**
     * - answeredSurvey 삭제
     * - blockedFriend 삭제
     * - dormantAccount 삭제
     * - 유저 스냅 사진 삭제
     * - 유저 프로필 삭제
     * - 메세지 연관 끊기
     * - 매칭 연관 끊기
     * - chatroomParticipant 연관 끊기
     * - refresh token 삭제
     */

    User user = userRepository.findById(userId)
            .orElseThrow(() -> new RingoException("User not found", HttpStatus.BAD_REQUEST));
    try {
      answeredSurveyRepository.deleteAllByUser(user);
      blockedFriendRepository.deleteByUser(user);
      dormantAccountRepository.deleteByUser(user);
      imageService.deleteAllProfileImagesByUser(user);
      imageService.deleteAllSnapImagesByUser(user);
      messageRepository.disconnectMessageWithUser(user);
      matchingRepository.disconnectRelationWithUserByInitRequestedUser(user);
      matchingRepository.disconnectRelationWithUserByInitRequestUser(user);
      chatroomParticipantRepository.disconnectChatroomParticipantWithUser(user);
      jwtRefreshTokenRepository.deleteByUser(user);
      userRepository.deleteById(userId);
    }catch (Exception e){
      throw new RingoException("유저 정보를 삭제하는데 실패하였습니다." + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  public String findUserId(Long userId){
    boolean isAuthenticated = (boolean) redisUtils.getDecryptKeyObject("self-auth" + userId);
    if(isAuthenticated){
      User user = userRepository.findById(userId)
          .orElseThrow(() -> new RingoException("유저를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST));
      return user.getUsername();
    }
    else{
      throw new RingoException("인증되지 않았습니다.", HttpStatus.FORBIDDEN);
    }
  }

  public void resetPassword(String password, Long userId){
    boolean isAuthenticated = (boolean) redisUtils.getDecryptKeyObject("self-auth" + userId);
    if(isAuthenticated){
      User user = userRepository.findById(userId)
          .orElseThrow(() -> new RingoException("유저를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST));
      user.setPassword(password);
      userRepository.save(user);
    }
    else{
      throw new RingoException("인증되지 않았습니다.", HttpStatus.FORBIDDEN);
    }
  }

  public GetUserInfoResponseDto getUserInfo(Long userId){
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RingoException("id에 해당하는 유저가 없습니다.", HttpStatus.BAD_REQUEST));
    return GetUserInfoResponseDto.builder()
        .id(user.getId())
        .gender(user.getGender().toString())
        .height(user.getHeight())
        .isDrinking(user.getIsDrinking().toString())
        .isSmoking(user.getIsSmoking().toString())
        .religion(user.getReligion().toString())
        .job(user.getJob())
        .nickname(user.getNickname())
        .build();
  }

  public void updateUserInfo(Long userId, UpdateUserInfoRequestDto dto){
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RingoException("id에 해당하는 유저가 없습니다.", HttpStatus.BAD_REQUEST));
    if (dto.height() != null && !dto.height().isEmpty())
      user.setHeight(dto.height());
    if (dto.isDrinking() != null && genericUtils.isContains(Drinking.values(), dto.isDrinking()))
      user.setIsDrinking(Drinking.valueOf(dto.isDrinking()));
    if (dto.isSmoking() != null && genericUtils.isContains(Smoking.values(), dto.isSmoking()))
      user.setIsSmoking(Smoking.valueOf(dto.isSmoking()));
    if (dto.religion() != null && genericUtils.isContains(Religion.values(), dto.religion()))
      user.setReligion(Religion.valueOf(dto.religion()));
    if (dto.job() != null && !dto.job().isEmpty())
      user.setJob(dto.job());

    userRepository.save(user);
  }



  public List<GetUserInfoResponseDto> getPageableUserInfo(int page, int size){
    Pageable pageable = PageRequest.of(page, size);
    Page<User> users = userRepository.findAll(pageable);
    return users.stream()
        .map(user -> {
            return GetUserInfoResponseDto.builder()
              .id(user.getId())
              .gender(user.getGender().toString())
              .height(user.getHeight())
              .isDrinking(user.getIsDrinking().toString())
              .isSmoking(user.getIsSmoking().toString())
              .religion(user.getReligion().toString())
              .job(user.getJob())
              .nickname(user.getNickname())
              .build();
        })
        .toList();
  }

  public void blockUser(Long userId, Long adminId){
    User admin = userRepository.findById(adminId)
        .orElseThrow(() -> new RingoException("id 에 해당하는 관리자가 없습니다.", HttpStatus.BAD_REQUEST));
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RingoException("id에 해당하는 유저가 없습니다.", HttpStatus.BAD_REQUEST));
    BlockedUser blockedUser = BlockedUser.builder()
        .phoneNumber(user.getPhoneNumber())
        .admin(admin)
        .build();
    blockedUserRepository.save(blockedUser);
  }

  public void saveUserAccessLog(User user){
    userAccessLogRepository.save(UserAccessLog.builder()
            .age(user.getAge())
            .userId(user.getId())
            .username(user.getUsername())
            .gender(user.getGender())
        .build());
  }

}
