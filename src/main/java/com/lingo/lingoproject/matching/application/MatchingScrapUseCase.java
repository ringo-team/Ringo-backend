package com.lingo.lingoproject.matching.application;

import com.lingo.lingoproject.matching.presentation.dto.GetScrappedUserResponseDto;
import com.lingo.lingoproject.matching.presentation.dto.GetUserProfileResponseDto;
import com.lingo.lingoproject.shared.domain.model.FaceVerify;
import com.lingo.lingoproject.shared.domain.model.Profile;
import com.lingo.lingoproject.shared.domain.model.ScrappedUser;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.infrastructure.persistence.ScrappedUserRepository;
import com.lingo.lingoproject.shared.utils.ApiListResponseDto;
import com.lingo.lingoproject.shared.utils.RedisUtils;
import com.lingo.lingoproject.user.application.UserQueryUseCase;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingScrapUseCase {

  private final MatchingRecommendationUseCase matchingRecommendationUseCase;
  private final ScrappedUserRepository scrappedUserRepository;
  private final UserQueryUseCase userQueryUseCase;

  private static final String DAILY_RECOMMENDATION_REDIS_KEY_PREFIX = "recommend-for-daily-survey::";
  private static final String CUMULATIVE_RECOMMENDATION_REDIS_KEY_PREFIX = "recommend::";
  private final RedisTemplate<String, Object> redisTemplate;
  private final RedisUtils redisUtils;


  @Transactional
  public void scrapUser(Long recommendedUserId, User user) {
    Long userId = user.getId();

    List<GetUserProfileResponseDto> cumulativeCachedProfile = matchingRecommendationUseCase.getCumulativeCachedProfile(user);
    updateScrapCache(cumulativeCachedProfile, userId, recommendedUserId, CUMULATIVE_RECOMMENDATION_REDIS_KEY_PREFIX);
    List<Long> cumulativeRecommendedIds = extractIdFromProfileDtos(cumulativeCachedProfile);
    log.info("step=스크랩_누적설문_추천목록, recommendedIds={}", cumulativeRecommendedIds);


    List<GetUserProfileResponseDto> dailyCachedProfile = matchingRecommendationUseCase.getDailyCachedProfile(user);
    updateScrapCache(dailyCachedProfile, userId, recommendedUserId, DAILY_RECOMMENDATION_REDIS_KEY_PREFIX);
    List<Long> dailyRecommendedIds = extractIdFromProfileDtos(dailyCachedProfile);
    log.info("step=스크랩_일일설문_추천목록, recommendedIds={}", dailyRecommendedIds);

    List<Long> allUserIds = new ArrayList<>();
    allUserIds.addAll(cumulativeRecommendedIds);
    allUserIds.addAll(dailyRecommendedIds);

    if (!allUserIds.contains(recommendedUserId)){
      log.warn("추천되지 않은 유저는 스크랩할 수 없습니다.");
      return;
    }

    User recommendedUser = userQueryUseCase.findUserOrThrow(recommendedUserId);

    if (scrappedUserRepository.existsByUserAndScrappedUser(user, recommendedUser)) {
      scrappedUserRepository.deleteByUserAndScrappedUser(user, recommendedUser);
      return;
    }
    scrappedUserRepository.save(ScrappedUser.of(user, recommendedUser));
  }

  private void updateScrapCache(
      List<GetUserProfileResponseDto> profiles,
      Long userId,
      Long recommendedUserId,
      String keyPrefix
  ){
    for (GetUserProfileResponseDto profile : profiles){
      if(profile.getUserId().equals(recommendedUserId)){
        if (profile.getIsScrap() == true) profile.setIsScrap(false);
        else profile.setIsScrap(true);
        break;
      }
    }
    redisUtils.cacheUntilMidnight(keyPrefix + userId, new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), profiles));
  }

  public List<GetScrappedUserResponseDto> getScrappedUsers(User user) {
    return scrappedUserRepository.findAllByUser(user)
        .stream()
        .map(ScrappedUser::getScrappedUser)
        .map(scrappedUser -> {
          Profile profile = scrappedUser.getProfile();
          return new GetScrappedUserResponseDto(
              scrappedUser.getId(),
              scrappedUser.getNickname(),
              LocalDate.now().getYear() - scrappedUser.getBirthday().getYear(),
              profile.getImageUrl(),
              FaceVerify.PASS == profile.getFaceVerify() ? 1 : 0
          );
        })
        .toList();
  }

  private List<Long> extractIdFromProfileDtos(List<GetUserProfileResponseDto> profiles) {
    return Optional.ofNullable(profiles)
        .orElseGet(Collections::emptyList)
        .stream()
        .map(GetUserProfileResponseDto::getUserId)
        .toList();
  }
}
