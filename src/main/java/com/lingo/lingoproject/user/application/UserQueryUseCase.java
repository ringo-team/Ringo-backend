package com.lingo.lingoproject.user.application;

import com.lingo.lingoproject.matching.presentation.dto.GetUserProfileResponseDto;
import com.lingo.lingoproject.shared.domain.model.FaceVerify;
import com.lingo.lingoproject.shared.domain.model.Hashtag;
import com.lingo.lingoproject.shared.domain.model.NotificationOptionOutUser;
import com.lingo.lingoproject.shared.domain.model.NotificationType;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import com.lingo.lingoproject.shared.infrastructure.persistence.HashtagRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.MatchingRepository;
import com.lingo.lingoproject.shared.domain.model.SignupStatus;
import com.lingo.lingoproject.shared.infrastructure.persistence.NotificationOptionOutUserRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.UserRepository;
import com.lingo.lingoproject.shared.utils.RedisUtils;
import com.lingo.lingoproject.user.presentation.dto.GetUserInfoResponseDto;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * 유저 조회 관련 Use Case
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserQueryUseCase {

  private final UserRepository userRepository;
  private final HashtagRepository hashtagRepository;
  private final RedisUtils redisUtils;
  private final RedisTemplate<String, Object> redisTemplate;
  private final MatchingRepository matchingRepository;
  private final DormantAccountUseCase dormantAccountUseCase;
  private final NotificationOptionOutUserRepository notificationOptionOutUserRepository;

  public String findUserLoginId() {
    throw new RingoException("아이디를 얻을 권한이 없습니다.", ErrorCode.NO_AUTH);
  }

  private List<Long> getCumulativeCachedUserIds(User user){
    return Optional.ofNullable(redisUtils.getCumulativeSurveyBasedCachedProfile(user.getId().toString()))
        .orElseGet(Collections::emptyList)
        .stream().map(GetUserProfileResponseDto::getUserId).toList();
  }
  private List<Long> getDailyCachedUserIds(User user){
    return Optional.ofNullable(redisUtils.getDailySurveyBasedCachedProfile(user.getId().toString()))
        .orElseGet(Collections::emptyList)
        .stream().map(GetUserProfileResponseDto::getUserId).toList();
  }

  private List<Long> getMatchingUserIds(User user){
    return matchingRepository.findAllByRequestUserOrRequestedUser(user, user)
        .stream()
        .map(m -> {
          User requestUser = m.getRequestUser();
          User requestedUser = m.getRequestedUser();
          return requestUser.getId().equals(user.getId()) ? requestedUser.getId() : requestUser.getId();
        })
        .toList();
  }
  public GetUserInfoResponseDto getUserInfo(Long findUserId, User user) {
    List<Long> cumulativeList = getCumulativeCachedUserIds(user);
    List<Long> dailyList = getDailyCachedUserIds(user);
    List<Long> matchingList = getMatchingUserIds(user);

    List<Long> userIdList = new ArrayList<>();
    userIdList.addAll(cumulativeList);
    userIdList.addAll(dailyList);
    userIdList.addAll(matchingList);

    log.info("cumulative-recommended: {}, daily-recommended: {}, find-user-id: {}",
        cumulativeList, dailyList, findUserId);

    boolean hasCommunityPass = redisTemplate.hasKey("membership::" + user.getId());

    if (!(userIdList.contains(findUserId) || findUserId.equals(user.getId()) || hasCommunityPass)) {
      throw new RingoException("유저를 조회할 권한이 없습니다.", ErrorCode.NO_AUTH);
    }

    User findUser = findUserOrThrow(findUserId);
    List<String> hashtags = getUserHashtags(findUser);
    boolean isDormant = dormantAccountUseCase.isDormant(findUser);
    Map<String, Boolean> notificationSettingMap = getNotificationSetting(findUser);
    boolean isFaceVerify = findUser.getProfile().getFaceVerify() == FaceVerify.PASS;
    log.info("[FACE]: {}", isFaceVerify);
    return GetUserInfoResponseDto.from(findUser, hashtags, isDormant, isFaceVerify, notificationSettingMap);
  }

  private Map<String, Boolean> getNotificationSetting(User user){
    Map<String, Boolean> notificationSettingMap = new HashMap<>();
    List<NotificationType> deactivateNotificationTypeList = notificationOptionOutUserRepository.findAllByUser(user)
        .stream()
        .map(NotificationOptionOutUser::getType)
        .toList();
    Arrays.stream(NotificationType.values())
        .forEach(type -> {
          // true 면 비활성화
          if(deactivateNotificationTypeList.contains(type)) notificationSettingMap.put(type.toString(), true);
          else notificationSettingMap.put(type.toString(), false);
        });
    return notificationSettingMap;
  }

  public List<GetUserInfoResponseDto> getPageableUserInfo(int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    Page<User> users = userRepository.findAll(pageable);
    return users.stream()
        .map(GetUserInfoResponseDto::summary)
        .toList();
  }

  public User findUserOrThrow(Long userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new RingoException(
            "해당 id의 유저를 찾을 수 없습니다.",
            ErrorCode.USER_NOT_FOUND));
  }

  private List<String> getUserHashtags(User user) {
    return hashtagRepository.findAllByUser(user)
        .stream()
        .map(Hashtag::getHashtag)
        .toList();
  }

  public Optional<User> findById(Long id) {
    return userRepository.findById(id);
  }

  public Optional<User> findByLoginId(String loginId) {
    return userRepository.findByLoginId(loginId);
  }

  public User findByLoginIdOrThrow(String loginId) {
    return userRepository.findByLoginId(loginId)
        .orElseThrow(() -> new RingoException("해당 loginId의 유저를 찾을 수 없습니다.", ErrorCode.USER_NOT_FOUND));
  }

  public List<User> findAllByIdIn(Collection<Long> ids) {
    return userRepository.findAllByIdIn(ids);
  }

  public List<User> findAll() {
    return userRepository.findAll();
  }

  public List<Long> findByUserIdNotInExcludedUserIds(List<Long> excludedUserIds) {
    return userRepository.findByUserIdNotInExcludedUserIds(excludedUserIds);
  }

  public List<User> findAllByStatusNot(SignupStatus status) {
    return userRepository.findAllByStatusNot(status);
  }

  public List<User> findAllByCreatedAtAfter(LocalDateTime createdAtAfter) {
    return userRepository.findAllByCreatedAtAfter(createdAtAfter);
  }

  public User save(User user) {
    return userRepository.save(user);
  }

  public void delete(User user) {
    userRepository.delete(user);
  }

  public boolean existsByLoginId(String loginId) {
    return userRepository.existsByLoginId(loginId);
  }

  public boolean existsByNickname(String nickname) {
    return userRepository.existsByNickname(nickname);
  }

  public Optional<User> findByFriendInvitationCode(String code) {
    return userRepository.findByFriendInvitationCode(code);
  }

  public Optional<User> findByPhoneNumber(String phoneNumber) {
    return userRepository.findByPhoneNumber(phoneNumber);
  }

  public Optional<User> findByLoginIdAndPhoneNumber(String loginId, String phoneNumber) {
    return userRepository.findByLoginIdAndPhoneNumber(loginId, phoneNumber);
  }
}
