package com.lingo.lingoproject.user.application;

import com.lingo.lingoproject.matching.presentation.dto.GetUserProfileResponseDto;
import com.lingo.lingoproject.shared.domain.model.Hashtag;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import com.lingo.lingoproject.shared.infrastructure.persistence.HashtagRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.MatchingRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.UserRepository;
import com.lingo.lingoproject.shared.utils.RedisUtils;
import com.lingo.lingoproject.user.presentation.dto.GetUserInfoResponseDto;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

  public String findUserLoginId(User user) {
    boolean isAuthenticated = redisTemplate.hasKey("self-auth::" + user.getId());
    if (isAuthenticated) return user.getUsername();
    throw new RingoException("아이디를 얻을 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
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
      throw new RingoException("유저를 조회할 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
    }

    User findUser = userRepository.findById(findUserId)
        .orElseThrow(() -> new RingoException("해당하는 유저가 존재하지 않습니다.", ErrorCode.NOT_FOUND_USER, HttpStatus.BAD_REQUEST));

    List<String> hashtags = hashtagRepository.findAllByUser(findUser)
        .stream().map(Hashtag::getHashtag).toList();

    return GetUserInfoResponseDto.from(findUser, hashtags);
  }

  public List<GetUserInfoResponseDto> getPageableUserInfo(int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    Page<User> users = userRepository.findAll(pageable);
    return users.stream()
        .map(GetUserInfoResponseDto::summary)
        .toList();
  }
}
