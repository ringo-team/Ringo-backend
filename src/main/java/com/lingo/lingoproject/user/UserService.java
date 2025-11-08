package com.lingo.lingoproject.user;

import com.lingo.lingoproject.domain.BlockedUser;
import com.lingo.lingoproject.domain.DormantAccount;
import com.lingo.lingoproject.domain.FriendInvitationLog;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.domain.UserAccessLog;
import com.lingo.lingoproject.domain.Withdrawer;
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
import com.lingo.lingoproject.repository.FcmTokenRepository;
import com.lingo.lingoproject.repository.FriendInvitationLogRepository;
import com.lingo.lingoproject.repository.JwtRefreshTokenRepository;
import com.lingo.lingoproject.repository.MatchingRepository;
import com.lingo.lingoproject.mongo_repository.MessageRepository;
import com.lingo.lingoproject.repository.UserAccessLogRepository;
import com.lingo.lingoproject.repository.UserPointRepository;
import com.lingo.lingoproject.repository.UserRepository;
import com.lingo.lingoproject.repository.WithdrawerRepository;
import com.lingo.lingoproject.user.dto.GetUserInfoResponseDto;
import com.lingo.lingoproject.user.dto.UpdateUserInfoRequestDto;
import com.lingo.lingoproject.utils.GenericUtils;
import com.lingo.lingoproject.utils.RedisUtils;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

  private final int FRIEND_INVITATION_REWARD = 10;
  private final int MAX_NUMBER_OF_INPUT = 5;

  private final UserRepository userRepository;
  private final RedisUtils redisUtils;
  private final BlockedUserRepository blockedUserRepository;
  private final AnsweredSurveyRepository answeredSurveyRepository;
  private final BlockedFriendRepository blockedFriendRepository;
  private final DormantAccountRepository dormantAccountRepository;
  private final ImageService imageService;
  private final MatchingRepository matchingRepository;
  private final JwtRefreshTokenRepository jwtRefreshTokenRepository;
  private final ChatroomParticipantRepository chatroomParticipantRepository;
  private final UserAccessLogRepository userAccessLogRepository;
  private final GenericUtils genericUtils;
  private final WithdrawerRepository withdrawerRepository;
  private final FriendInvitationLogRepository friendInvitationLogRepository;
  private final UserPointRepository userPointRepository;
  private final FcmTokenRepository fcmTokenRepository;

  @Transactional
  public void deleteUser(Long userId, String reason) {

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RingoException("User not found", HttpStatus.BAD_REQUEST));
    withdrawerRepository.save(Withdrawer
        .builder()
        .joinPeriod(user.getCreatedAt())
        .reason(reason)
        .build());

    try {
      answeredSurveyRepository.deleteAllByUser(user);
      blockedFriendRepository.deleteByUser(user);
      dormantAccountRepository.deleteByUser(user);
      imageService.deleteAllProfileImagesByUser(user);
      imageService.deleteAllSnapImagesByUser(user);
      matchingRepository.deleteAllByRequestedUser(user);
      matchingRepository.deleteAllByRequestUser(user);
      fcmTokenRepository.deleteByUser(user);
      chatroomParticipantRepository.disconnectChatroomParticipantWithUser(user);
      userPointRepository.deleteAllByUser(user);
      jwtRefreshTokenRepository.deleteByUser(user);
      userRepository.deleteById(userId);
    } catch (Exception e) {
      throw new RingoException("유저 정보를 삭제하는데 실패하였습니다." + e.getMessage(),
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  public String findUserId(Long userId) {
    boolean isAuthenticated = (boolean) redisUtils.getDecryptKeyObject("self-auth" + userId);
    if (isAuthenticated) {
      User user = userRepository.findById(userId)
          .orElseThrow(() -> new RingoException("유저를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST));
      return user.getUsername();
    } else {
      throw new RingoException("인증되지 않았습니다.", HttpStatus.FORBIDDEN);
    }
  }

  public void resetPassword(String password, Long userId) {
    boolean isAuthenticated = (boolean) redisUtils.getDecryptKeyObject("self-auth" + userId);
    if (isAuthenticated) {
      User user = userRepository.findById(userId)
          .orElseThrow(() -> new RingoException("유저를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST));
      user.setPassword(password);
      userRepository.save(user);
    } else {
      throw new RingoException("인증되지 않았습니다.", HttpStatus.FORBIDDEN);
    }
  }

  public GetUserInfoResponseDto getUserInfo(Long userId) {
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
        .biography(user.getBiography())
        .build();
  }

  public void updateUserInfo(Long userId, UpdateUserInfoRequestDto dto) {
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
    if (dto.biography() != null && !dto.biography().isEmpty())
      user.setBiography(dto.biography());

    userRepository.save(user);
  }


  public List<GetUserInfoResponseDto> getPageableUserInfo(int page, int size) {
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
              .biography(user.getBiography())
              .build();
        })
        .toList();
  }

  public void blockUser(Long userId, Long adminId) {
    User admin = userRepository.findById(adminId)
        .orElseThrow(() -> new RingoException("id 에 해당하는 관리자가 없습니다.", HttpStatus.BAD_REQUEST));
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RingoException("id에 해당하는 유저가 없습니다.", HttpStatus.BAD_REQUEST));
    BlockedUser blockedUser = BlockedUser.builder()
        .blockedUserId(user.getId())
        .phoneNumber(user.getPhoneNumber())
        .admin(admin)
        .build();
    blockedUserRepository.save(blockedUser);
  }

  public void saveUserAccessLog(User user) {
    userAccessLogRepository.save(UserAccessLog.builder()
        .age(user.getAge())
        .userId(user.getId())
        .username(user.getUsername())
        .gender(user.getGender())
        .build());
  }

  /**
   * 친구초대 코드를 입력했을 때 보상을 제공하는 함수이다. 만약 친구초대로 보상을 이미 받았거나 5번 이상 친구초대코드 입력에 실패했을 경우 예외를 발생시킨다. 만약
   * 친구초대코드가 정말 존재하는 코드라면 host id와 friend Id를 db에 저장한 후 리워드를 제공한다.
   *
   * @param user
   * @param code
   */
  @Transactional
  public void checkFriendInvitationCodeAndProvideReward(User user, String code) {
    // 이미 친구초대코드로 보상을 받았거나
    // 친구초대코드 입력 횟수를 초과했을 경우
    // 보상을 받을 수 없게 막아놓음
    if (friendInvitationLogRepository.getNumberOfParticipation(user.getId(), true) > 0 ||
        friendInvitationLogRepository.getNumberOfParticipation(user.getId(), false)
            > MAX_NUMBER_OF_INPUT) {
      throw new RingoException("이미 완료한 이벤트거나 입력횟수를 초과하였습니다.", HttpStatus.BAD_REQUEST);
    }
    Optional<User> host = userRepository.findByFriendInvitationCode(code);
    // 친구초대코드를 잘못 입력하였을 경우
    // 로그를 남겨 입력 횟수를 기록함
    if (host.isEmpty()) {
      friendInvitationLogRepository.save(FriendInvitationLog.builder()
          .hostId(null)
          .friendId(user.getId())
          .isRealCode(false)
          .build());
      throw new RingoException("친구초대코드를 잘못 입력하였습니다.", HttpStatus.BAD_REQUEST);
    } else {
      friendInvitationLogRepository.save(FriendInvitationLog.builder()
          .hostId(host.get().getId())
          .friendId(user.getId())
          .isRealCode(true)
          .build());
      userPointRepository.updateUserPoint(FRIEND_INVITATION_REWARD, user);
      userPointRepository.updateUserPoint(FRIEND_INVITATION_REWARD, host.get());
    }
  }

  public void updateDormantAccount(User user, String request){
    if(request.equals("DORMANT")){
      if(dormantAccountRepository.existsByUser(user)){
        throw new RingoException("이미 휴면 중인 계정입니다.", HttpStatus.BAD_REQUEST);
      }
      DormantAccount dormantAccount = DormantAccount.builder()
          .user(user)
          .build();
      dormantAccountRepository.save(dormantAccount);
    }
    else{
      dormantAccountRepository.deleteByUser(user);
    }
  }
}
