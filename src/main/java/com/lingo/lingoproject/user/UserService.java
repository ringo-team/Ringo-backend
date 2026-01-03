package com.lingo.lingoproject.user;

import com.lingo.lingoproject.domain.BlockedUser;
import com.lingo.lingoproject.domain.DormantAccount;
import com.lingo.lingoproject.domain.FriendInvitationLog;
import com.lingo.lingoproject.domain.Profile;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.domain.UserAccessLog;
import com.lingo.lingoproject.domain.Withdrawer;
import com.lingo.lingoproject.domain.enums.Drinking;
import com.lingo.lingoproject.domain.enums.Religion;
import com.lingo.lingoproject.domain.enums.Smoking;
import com.lingo.lingoproject.exception.ErrorCode;
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
import com.lingo.lingoproject.repository.ProfileRepository;
import com.lingo.lingoproject.repository.UserAccessLogRepository;
import com.lingo.lingoproject.repository.UserPointRepository;
import com.lingo.lingoproject.repository.UserRepository;
import com.lingo.lingoproject.repository.WithdrawerRepository;
import com.lingo.lingoproject.user.dto.GetUserInfoResponseDto;
import com.lingo.lingoproject.user.dto.UpdateUserInfoRequestDto;
import com.lingo.lingoproject.utils.GenericUtils;
import com.lingo.lingoproject.utils.RedisUtils;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
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
  private final ProfileRepository profileRepository;

  @Transactional
  public void deleteUser(User user, String reason) {

    try {

      withdrawerRepository.save(Withdrawer
          .builder()
          .joinPeriod(ChronoUnit.DAYS.between(user.getCreatedAt(), LocalDate.now()))
          .reason(reason)
          .build());

      answeredSurveyRepository.deleteAllByUser(user);
      blockedFriendRepository.deleteByUser(user);
      dormantAccountRepository.deleteByUser(user);
      imageService.deleteProfileImageByUser(user);
      imageService.deleteAllSnapImagesByUser(user);
      matchingRepository.deleteAllByRequestedUser(user);
      matchingRepository.deleteAllByRequestUser(user);
      fcmTokenRepository.deleteByUser(user);
      chatroomParticipantRepository.disconnectChatroomParticipantWithUser(user);
      userPointRepository.deleteAllByUser(user);
      jwtRefreshTokenRepository.deleteByUser(user);
      userRepository.delete(user);
    } catch (Exception e) {
      log.error("유저 데이터 삭제 실패. userId: {}, reason: {}", user.getId(), reason, e);
      throw new RingoException("유저 정보를 삭제하는데 실패하였습니다." + e.getMessage(),
          ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  public String findUserLoginId(User user) {
    Long userId = user.getId();
    boolean isAuthenticated = redisUtils.isCompleteSelfAuth(userId.toString());
    if (isAuthenticated) {
      return user.getUsername();
    } else {
      throw new RingoException("아이디를 얻을 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
    }
  }

  public void resetPassword(String password, User user) {
    Long userId = user.getId();

    boolean isAuthenticated = redisUtils.isCompleteSelfAuth(userId.toString());
    if (isAuthenticated) {
      user.setPassword(password);
      userRepository.save(user);
    } else {
      throw new RingoException("비밀번호를 재설정할 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
    }
  }

  public GetUserInfoResponseDto getUserInfo(User user) {

    Profile profile = profileRepository.findByUser(user)
        .orElseThrow(() -> new RingoException("유저 정보 조회 중 프로필을 찾을 수 없습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR));

    return GetUserInfoResponseDto.builder()
        .userId(user.getId())
        .profile(profile.getImageUrl())
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

  public void updateUserInfo(User user, UpdateUserInfoRequestDto dto) {

    String drinking = dto.isDrinking();
    String smoking  = dto.isSmoking();
    String religion = dto.religion();
    String height   = dto.height();
    String job      = dto.job();
    String biography = dto.biography();

    genericUtils.validateAndSetEnum(drinking, Drinking.values(), user::setIsDrinking, Drinking.class);
    genericUtils.validateAndSetEnum(smoking, Smoking.values(), user::setIsSmoking, Smoking.class);
    genericUtils.validateAndSetEnum(religion, Religion.values(), user::setReligion, Religion.class);

    genericUtils.validateAndSetStringValue(height, user::setHeight);
    genericUtils.validateAndSetStringValue(job, user::setJob);
    genericUtils.validateAndSetStringValue(biography, user::setBiography);

    userRepository.save(user);
  }


  public List<GetUserInfoResponseDto> getPageableUserInfo(int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    Page<User> users = userRepository.findAll(pageable);
    return users.stream()
        .map(user ->
          GetUserInfoResponseDto.builder()
              .userId(user.getId())
              .gender(user.getGender().toString())
              .height(user.getHeight())
              .isDrinking(user.getIsDrinking().toString())
              .isSmoking(user.getIsSmoking().toString())
              .religion(user.getReligion().toString())
              .job(user.getJob())
              .nickname(user.getNickname())
              .biography(user.getBiography())
              .build()
        )
        .toList();
  }

  public void blockUser(Long userId, Long adminId) {
    User admin = userRepository.findById(adminId)
        .orElseThrow(() -> new RingoException("id 에 해당하는 관리자가 없습니다.", ErrorCode.NOT_FOUND_USER, HttpStatus.BAD_REQUEST));
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RingoException("id에 해당하는 유저가 없습니다.", ErrorCode.NOT_FOUND_ADMIN, HttpStatus.BAD_REQUEST));
    BlockedUser blockedUser = BlockedUser.builder()
        .blockedUserId(user.getId())
        .phoneNumber(user.getPhoneNumber())
        .admin(admin)
        .build();
    blockedUserRepository.save(blockedUser);
  }

  public void saveUserAccessLog(User user) {
    // 오늘 유저가 접속했으면 접속 정보를 추가로 저장하지 않는다.
    if (userAccessLogRepository.existsByUserIdAndCreateAtAfter(user.getId(), LocalDate.now().atStartOfDay())){
      return;
    }
    userAccessLogRepository.save(UserAccessLog.builder()
        .age(user.getAge())
        .userId(user.getId())
        .username(user.getUsername())
        .gender(user.getGender())
        .build());
  }

  public void updateUserProfileVerification(User user){
    Profile profile = profileRepository.findByUser(user)
        .orElseThrow(() -> new RingoException(
            "userId=" + user.getId() + "프로필 검증 업데이트 중 프로필을 찾을 수 없는 오류가 발생했습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR));
    profile.setIsVerified(true);
    profileRepository.save(profile);
  }

  /**
   * 친구초대 코드를 입력했을 때 보상을 제공하는 함수이다. 만약 친구초대로 보상을 이미 받았거나 5번 이상 친구초대코드 입력에 실패했을 경우 예외를 발생시킨다.
   * 만약 친구초대코드가 정말 존재하는 코드라면 host id와 friend id를 db에 저장한 후 리워드를 제공한다.
   */
  @Transactional
  public void checkFriendInvitationCodeAndProvideReward(User user, String code) {
    // 이미 친구초대코드로 보상을 받았거나
    // 친구초대코드 입력 횟수를 초과했을 경우
    // 보상을 받을 수 없게 막아놓음
    if (friendInvitationLogRepository.getNumberOfParticipation(user.getId(), true) > 0 ||
        friendInvitationLogRepository.getNumberOfParticipation(user.getId(), false)
            > MAX_NUMBER_OF_INPUT) {
      throw new RingoException("이미 완료한 이벤트거나 입력횟수를 초과하였습니다.", ErrorCode.DUPLICATED, HttpStatus.BAD_REQUEST);
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
      throw new RingoException("친구초대코드를 잘못 입력하였습니다.", ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST);
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

  public void updateDormantAccount(User user){
    if(dormantAccountRepository.existsByUser(user)){
      dormantAccountRepository.deleteByUser(user);
      return;
    }
    DormantAccount dormantAccount = DormantAccount.builder()
        .user(user)
        .build();
    dormantAccountRepository.save(dormantAccount);
  }
}
