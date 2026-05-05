package com.lingo.lingoproject.matching.application;

import com.lingo.lingoproject.matching.domain.service.RecommendationDomainService;
import com.lingo.lingoproject.matching.domain.service.SurveyScoreCalculator;
import com.lingo.lingoproject.matching.presentation.dto.GetUserProfileResponseDto;
import com.lingo.lingoproject.shared.domain.model.AnsweredSurvey;
import com.lingo.lingoproject.shared.domain.model.FaceVerify;
import com.lingo.lingoproject.shared.domain.model.Hashtag;
import com.lingo.lingoproject.shared.domain.model.Profile;
import com.lingo.lingoproject.shared.domain.model.ScrappedUser;
import com.lingo.lingoproject.shared.domain.model.SignupStatus;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.domain.model.UserActivityLog;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.infrastructure.persistence.AnsweredSurveyRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.BlockedFriendRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.BlockedUserRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.DormantAccountRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.HashtagRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.ProfileRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.ScrappedUserRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.UserActivityLogRepository;
import com.lingo.lingoproject.shared.utils.ApiListResponseDto;
import com.lingo.lingoproject.shared.utils.RedisUtils;
import com.lingo.lingoproject.user.application.UserQueryUseCase;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingRecommendationUseCase {

  private final MatchQueryUseCase matchQueryUseCase;
  private final UserQueryUseCase userQueryUseCase;
  private final BlockedFriendRepository blockedFriendRepository;
  private final DormantAccountRepository dormantAccountRepository;
  private final ProfileRepository profileRepository;
  private final AnsweredSurveyRepository answeredSurveyRepository;
  private final SurveyScoreCalculator surveyScoreCalculator;
  private final RecommendationDomainService recommendationDomainService;
  private final HashtagRepository hashtagRepository;
  private final BlockedUserRepository blockedUserRepository;
  private final RedisUtils redisUtils;
  private final UserActivityLogRepository userActivityLogRepository;
  private final RedisTemplate<String, Object> redisTemplate;
  private final ScrappedUserRepository scrappedUserRepository;

  private static final int MAX_CUMULATIVE_SURVEY_BASED_RECOMMENDATION_SIZE = 4;
  private static final int MAX_DAILY_SURVEY_BASED_RECOMMENDATION_SIZE = 4;
  private static final float SURVEY_SCORE_THRESHOLD = 0;
  private static final int ACTIVE_DAY_DURATION = 14;
  private static final int HIDE_PROFILE_FLAG = 1;
  private static final int EXPOSE_PROFILE_FLAG = 0;
  private static final int PROFILE_VERIFICATION_FLAG = 1;
  private static final int PROFILE_NON_VERIFICATION_FLAG = 0;
  private static final String REDIS_ACTIVE_USER_IDS = "redis::active::ids";
  private static final String DAILY_RECOMMENDATION_REDIS_KEY_PREFIX = "recommend-for-daily-survey::";
  private static final String CUMULATIVE_RECOMMENDATION_REDIS_KEY_PREFIX = "recommend::";

  @Transactional
  public List<GetUserProfileResponseDto> getCumulativeSurveyBasedRecommendationProfiles(User user) {
    Long userId = user.getId();

    List<GetUserProfileResponseDto> cached = getCumulativeCachedProfile(user);
    if (cached != null && cached.size() == MAX_CUMULATIVE_SURVEY_BASED_RECOMMENDATION_SIZE) {
      log.info("step=누적설문_추천_캐시_히트, userId={}, cacheSize={}", userId, cached.size());
      return cached;
    }

    List<GetUserProfileResponseDto> result = getFirstUserCumulativeProfile(user);

    redisUtils.cacheUntilMidnight(
        CUMULATIVE_RECOMMENDATION_REDIS_KEY_PREFIX + userId,
        new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), result)
    );

    return result;
  }

  @Scheduled(cron = "0 40 23 * * * ")
  public void cacheCumulativeProfile() {
    List<User> users = userQueryUseCase.findAll();
    users.forEach(this::cacheEachCumulativeProfile);
  }

  @Transactional
  void cacheEachCumulativeProfile(User user) {
    Long userId = user.getId();

    List<Long> activeUserIds = getActiveUserIds(user);
    Map<Long, Float> candidates = getUserAboveSurveyScore(userId, activeUserIds);
    List<Long> selectedIds = selectCandidatesByScore(candidates);
    log.info("step=누적설문_추천_대상_선정, selectedIds={}", selectedIds);

    List<GetUserProfileResponseDto> result = getUserProfilesByIds(user, selectedIds);

    redisUtils.cacheUntilMidnight(
        CUMULATIVE_RECOMMENDATION_REDIS_KEY_PREFIX + userId,
        new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), result)
    );
  }

  List<GetUserProfileResponseDto> getFirstUserCumulativeProfile(User user) {
    List<Long> excludedUserIds = getRecommendationExcludedUserIds(user);
    List<Long> userIds = userQueryUseCase.findByUserIdNotInExcludedUserIds(excludedUserIds);
    List<Long> randomlySelectedUserIds = recommendationDomainService.selectRandomCandidates(userIds, 5);

    return userQueryUseCase.findAllByIdIn(randomlySelectedUserIds)
        .stream()
        .map(u -> buildUserProfileDto(user, u))
        .toList();
  }

  public List<GetUserProfileResponseDto> getDailySurveyBasedRecommendationProfiles(User user) {
    List<GetUserProfileResponseDto> cached = getDailyCachedProfile(user);
    if (cached != null && cached.size() == MAX_DAILY_SURVEY_BASED_RECOMMENDATION_SIZE) {
      return cached;
    }
    return cacheEachDailyProfile(user);
  }

  @Scheduled(cron = "0 50 23 * * *")
  public void cacheDailyProfile() {
    List<User> users = userQueryUseCase.findAll();
    users.forEach(this::cacheEachDailyProfile);
  }

  List<GetUserProfileResponseDto> cacheEachDailyProfile(User user) {
    List<GetUserProfileResponseDto> result = getEachDailyProfile(user);

    if (result == null) return null;

    redisUtils.cacheUntilMidnight(
        DAILY_RECOMMENDATION_REDIS_KEY_PREFIX + user.getId(),
        new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), result)
    );

    return result;
  }

  public void hideRecommendedUser(User user, Long recommendedUserId) {
    applyHideFlagToCache(
        CUMULATIVE_RECOMMENDATION_REDIS_KEY_PREFIX,
        user,
        recommendedUserId,
        this::getCumulativeCachedProfile
    );
    applyHideFlagToCache(
        DAILY_RECOMMENDATION_REDIS_KEY_PREFIX,
        user,
        recommendedUserId,
        this::getDailyCachedProfile
    );
  }

  @Transactional
  public void refreshActiveUserCache() {
    LocalDateTime cutoff = LocalDate.now().minusDays(ACTIVE_DAY_DURATION).atStartOfDay();
    Set<Long> activeUserIds = userActivityLogRepository.findAllByStartAfter(cutoff)
        .stream()
        .map(UserActivityLog::getUser)
        .map(User::getId)
        .collect(Collectors.toSet());
    Set<Long> connectedUserIds = redisTemplate.keys("connect-app::*")
        .stream()
        .map(s -> Long.parseLong(s.split("::")[1]))
        .collect(Collectors.toSet());
    activeUserIds.addAll(connectedUserIds);
    List<Long> allActiveUserIds = activeUserIds.stream().toList();
    redisTemplate.delete(REDIS_ACTIVE_USER_IDS);
    redisTemplate.opsForValue().set(
        REDIS_ACTIVE_USER_IDS,
        new ApiListResponseDto<>(ErrorCode.SUCCESS.toString(), allActiveUserIds),
        5,
        TimeUnit.MINUTES
    );
  }

  public List<Long> getRecommendationExcludedUserIds(User user) {
    Long userId = user.getId();

    List<Long> blockedFriendIds = blockedFriendRepository.findUsersMutuallyBlockedWith(userId);
    List<Long> dormantUserIds = getDormantUserIds();
    List<Long> blockedUserIds = getBlockedUserIds();
    List<Long> suspendedUserIds = getSuspendedUserIds();
    List<Long> usersWhoRequestedMe = getUserWhoRequestedMe(user);
    List<Long> usersIRequested = getUsersIRequested(user);
    List<Long> signupIncompleteUserIds = getSignupIncompleteUsersIds();

    Set<Long> excluded = new HashSet<>();
    excluded.add(userId);
    excluded.addAll(blockedFriendIds);
    excluded.addAll(dormantUserIds);
    excluded.addAll(blockedUserIds);
    excluded.addAll(suspendedUserIds);
    excluded.addAll(usersWhoRequestedMe);
    excluded.addAll(usersIRequested);
    excluded.addAll(signupIncompleteUserIds);

    log.info("step=추천_제외_유저_수집, excludedUserIds={}", excluded);
    return new ArrayList<>(excluded);
  }

  @Transactional
  public void updateProfileClickCount(User user) {
    profileRepository.updateProfileClickCount(user.getProfile().getId());
  }

  public List<GetUserProfileResponseDto> getCumulativeCachedProfile(User user) {
    return getCachedRecommendationProfiles(
        CUMULATIVE_RECOMMENDATION_REDIS_KEY_PREFIX,
        redisUtils::getCumulativeSurveyBasedCachedProfile,
        user
    );
  }

  public List<GetUserProfileResponseDto> getDailyCachedProfile(User user) {
    return getCachedRecommendationProfiles(
        DAILY_RECOMMENDATION_REDIS_KEY_PREFIX,
        redisUtils::getDailySurveyBasedCachedProfile,
        user
    );
  }

  @Transactional
  List<Long> getActiveUserIds(User user) {
    List<Long> activeUserIds = getCachedActiveUserIds();
    if (activeUserIds.isEmpty()) {
      refreshActiveUserCache();
      activeUserIds = getCachedActiveUserIds();
    }
    log.info("step=누적설문_추천_활성유저_조회, activeUserCount={}", activeUserIds);

    List<Long> excludedUserIds = getRecommendationExcludedUserIds(user);
    log.info("step=누적설문_추천_제외유저_조회, excludedUserCount={}", excludedUserIds.size());

    activeUserIds.removeAll(excludedUserIds);
    log.info("step=누적설문_추천_풀_확정, candidateCount={}", activeUserIds.size());

    return activeUserIds;
  }

  private List<GetUserProfileResponseDto> getEachDailyProfile(User user) {
    List<AnsweredSurvey> todayAnswers = getTodayAnsweredSurveys(user);
    if (todayAnswers.isEmpty()) {
      log.info("step=금일_설문_응답_부재, todayAnswersSize=0");
      return null;
    }

    Collections.shuffle(todayAnswers);

    List<Long> excludedUserIds = getRecommendationExcludedUserIds(user);
    log.info("step=일일설문_추천_제외유저_조회, excludedUserCount={}", excludedUserIds.size());

    return getSimilarAnsweredUserProfiles(user, todayAnswers, excludedUserIds);
  }

  private List<GetUserProfileResponseDto> getSimilarAnsweredUserProfiles(
      User user,
      List<AnsweredSurvey> todayAnswers,
      List<Long> excludedUserIds
  ) {
    Set<GetUserProfileResponseDto> result = new HashSet<>();
    for (AnsweredSurvey todayAnswer : todayAnswers) {
      if (result.size() == MAX_DAILY_SURVEY_BASED_RECOMMENDATION_SIZE) break;

      User recommendedUser = getRandomlySimilarAnsweredUser(todayAnswer, excludedUserIds);
      if (recommendedUser == null) continue;

      result.add(buildUserProfileDto(user, recommendedUser));
    }
    return new ArrayList<>(result);
  }

  private User getRandomlySimilarAnsweredUser(AnsweredSurvey todayAnswer, List<Long> excludedUserIds) {
    List<AnsweredSurvey> similarAnswers = getSimilarAnswers(todayAnswer, excludedUserIds);

    if (similarAnswers.isEmpty()) return null;

    int index = ThreadLocalRandom.current().nextInt(similarAnswers.size());
    User recommendedUser = similarAnswers.get(index).getUser();

    logDailySurveyBasedRecommendationLogic(
        todayAnswer,
        similarAnswers.get(index),
        todayAnswer.getUser(),
        recommendedUser
    );

    return recommendedUser;
  }

  private List<GetUserProfileResponseDto> getUserProfilesByIds(User requestUser, List<Long> selectedIds) {
    List<User> users = userQueryUseCase.findAllByIdIn(selectedIds);
    Map<Long, List<String>> hashtagsMap = batchGetHashtagsByUsers(users);
    Set<Long> scrappedUserIds = scrappedUserRepository.findAllByUser(requestUser).stream()
        .map(s -> s.getScrappedUser().getId())
        .collect(Collectors.toSet());
    Map<Long, Float> surveyScoreMap = surveyScoreCalculator.batchCalculate(requestUser.getId(), selectedIds);

    return users.stream()
        .map(recommended -> {
          Profile profile = recommended.getProfile();
          float surveyScore = surveyScoreMap.get(recommended.getId());
          int isVerify = FaceVerify.PASS == profile.getFaceVerify() ? PROFILE_VERIFICATION_FLAG : PROFILE_NON_VERIFICATION_FLAG;
          List<String> hashtags = hashtagsMap.getOrDefault(recommended.getId(), List.of());
          boolean isScrap = scrappedUserIds.contains(recommended.getId());

          GetUserProfileResponseDto dto = GetUserProfileResponseDto.of(
              recommended,
              surveyScore,
              hashtags,
              isVerify,
              EXPOSE_PROFILE_FLAG,
              requestUser.getMbti(),
              isScrap
          );
          log.info("step=추천이성_프로필_빌드, recommendedUserId={}, age={}, gender={}, nickname={}, matchingScore={}, hashtag={}, faceVerified={}, daysFromLastAccess={}, mbti={}",
              dto.getUserId(), dto.getAge(), dto.getGender(), dto.getNickname(),
              dto.getMatchingScore(), dto.getHashtags(), dto.getVerify(),
              dto.getDaysFromLastAccess(), dto.getMbti());
          return dto;
        })
        .toList();
  }

  private List<Long> selectCandidatesByScore(Map<Long, Float> scoreMap) {
    Map<Long, Double> finalScoreMap = recommendationDomainService.calculateFinalMatchingScore(scoreMap);
    return finalScoreMap
        .entrySet()
        .stream()
        .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
        .map(Map.Entry::getKey)
        .limit(MAX_CUMULATIVE_SURVEY_BASED_RECOMMENDATION_SIZE)
        .toList();
  }

  private Map<Long, Float> getUserAboveSurveyScore(Long userId, List<Long> activeUserIds) {
    return surveyScoreCalculator.batchCalculate(userId, activeUserIds)
        .entrySet()
        .stream()
        .filter(entry -> entry.getValue() >= SURVEY_SCORE_THRESHOLD)
        .sorted((entry1, entry2) -> Float.compare(entry2.getValue(), entry1.getValue()))
        .limit(100)
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }

  private List<GetUserProfileResponseDto> getCachedRecommendationProfiles(
      String keyPrefix,
      Function<String, List<GetUserProfileResponseDto>> function,
      User user
  ) {
    Long userId = user.getId();
    if (redisTemplate.hasKey(keyPrefix + userId)) {
      List<GetUserProfileResponseDto> cached = function.apply(userId.toString());
      updateIsScrapFlag(user, cached, keyPrefix);
      logCachedProfiles(user, cached);
      return cached;
    }
    return null;
  }

  private void updateIsScrapFlag(User user, List<GetUserProfileResponseDto> profiles, String keyPrefix) {
    Set<Long> scrappedUserIds = scrappedUserRepository.findAllByUser(user).stream()
        .map(s -> s.getScrappedUser().getId())
        .collect(Collectors.toSet());
    profiles.forEach(profile -> profile.setScrap(scrappedUserIds.contains(profile.getUserId())));
    redisUtils.cacheUntilMidnight(
        keyPrefix + user.getId(),
        new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), profiles)
    );
  }

  private void applyHideFlagToCache(
      String redisKeyPrefix,
      User user,
      Long targetUserId,
      Function<User, List<GetUserProfileResponseDto>> function
  ) {
    List<GetUserProfileResponseDto> profiles = function.apply(user);
    if (profiles == null) return;
    markProfileAsHidden(profiles, targetUserId, user.getId());
    redisUtils.cacheUntilMidnight(
        redisKeyPrefix + user.getId(),
        new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), profiles)
    );
  }

  private void markProfileAsHidden(List<GetUserProfileResponseDto> profiles, Long targetUserId, Long requestUserId) {
    for (GetUserProfileResponseDto profile : profiles) {
      if (profile.getUserId().equals(targetUserId)) {
        profile.setHide(HIDE_PROFILE_FLAG);
        log.info("step=추천이성_숨김, requestUserId={}, hiddenUserId={}", requestUserId, targetUserId);
        return;
      }
    }
  }

  @SuppressWarnings("unchecked")
  private List<Long> getCachedActiveUserIds() {
    Object cache = redisTemplate.opsForValue().get(REDIS_ACTIVE_USER_IDS);
    if (cache instanceof ApiListResponseDto<?> dto && dto.getList() != null) {
      return new ArrayList<>((List<Long>) dto.getList());
    }
    return new ArrayList<>();
  }

  private GetUserProfileResponseDto buildUserProfileDto(User requestUser, User recommendedUser) {
    Profile profile = recommendedUser.getProfile();
    float surveyScore = surveyScoreCalculator.calculate(requestUser.getId(), recommendedUser.getId());
    int isVerify = FaceVerify.PASS == profile.getFaceVerify() ? PROFILE_VERIFICATION_FLAG : PROFILE_NON_VERIFICATION_FLAG;
    List<String> hashtags = getUserHashtags(recommendedUser);
    boolean isScrap = scrappedUserRepository.existsByUserAndScrappedUser(requestUser, recommendedUser);

    GetUserProfileResponseDto dto = GetUserProfileResponseDto.of(
        recommendedUser,
        surveyScore,
        hashtags,
        isVerify,
        EXPOSE_PROFILE_FLAG,
        requestUser.getMbti(),
        isScrap
    );

    log.info("step=추천이성_프로필_빌드, recommendedUserId={}, age={}, gender={}, nickname={}, matchingScore={}, hashtag={}, faceVerified={}, daysFromLastAccess={}, mbti={}",
        dto.getUserId(), dto.getAge(), dto.getGender(), dto.getNickname(),
        dto.getMatchingScore(), dto.getHashtags(), dto.getVerify(),
        dto.getDaysFromLastAccess(), dto.getMbti());

    return dto;
  }

  private Map<Long, List<String>> batchGetHashtagsByUsers(List<User> users) {
    return hashtagRepository.findAllByUserIn(users).stream()
        .collect(Collectors.groupingBy(
            h -> h.getUser().getId(),
            Collectors.mapping(Hashtag::getHashtag, Collectors.toList())
        ));
  }

  private List<String> getUserHashtags(User user) {
    return hashtagRepository.findAllByUser(user)
        .stream()
        .map(Hashtag::getHashtag)
        .toList();
  }

  private List<AnsweredSurvey> getSimilarAnswers(AnsweredSurvey todayAnswer, List<Long> excludedUserIds) {
    int surveyNum = todayAnswer.getSurveyNum();
    return answeredSurveyRepository.findAllByUserIdNotInAndAnswerAndSurveyNumIn(
        excludedUserIds,
        todayAnswer.getAnswer(),
        List.of(surveyNum, surveyNum + 1, surveyNum - 1)
    );
  }

  private List<AnsweredSurvey> getTodayAnsweredSurveys(User user) {
    return answeredSurveyRepository.findAllByUserAndUpdatedAtAfter(
        user, LocalDate.now().atStartOfDay()
    );
  }

  private List<Long> getSignupIncompleteUsersIds() {
    return userQueryUseCase.findAllByStatusNot(SignupStatus.COMPLETED)
        .stream()
        .map(User::getId)
        .toList();
  }

  private List<Long> getDormantUserIds() {
    return dormantAccountRepository.findAllDormantUserIds();
  }

  private List<Long> getBlockedUserIds() {
    return blockedUserRepository.findAllBlockedUserIds();
  }

  private List<Long> getSuspendedUserIds() {
    return redisTemplate.keys("suspension::*")
        .stream()
        .map(s -> s.replace("suspension::", ""))
        .map(Long::parseLong)
        .toList();
  }

  private List<Long> getUsersIRequested(User user) {
    return matchQueryUseCase.findRequestedUserIdsByRequestUser(user);
  }

  private List<Long> getUserWhoRequestedMe(User user) {
    return matchQueryUseCase.findRequestUserIdsByRequestedUser(user);
  }

  private void logDailySurveyBasedRecommendationLogic(
      AnsweredSurvey todayAnswer,
      AnsweredSurvey selectedAnswer,
      User user,
      User recommended
  ) {
    log.info("""
            step=일일설문_추천_매칭,
            selectedSurveyId={} | surveyNum={},
            requestUserId={} | recommendedUserId={}
            requestUserAnswer={} | recommendedUserAnswer={}
            """,
        selectedAnswer.getId(), selectedAnswer.getSurveyNum(),
        user.getId(), recommended.getId(),
        todayAnswer.getAnswer(), selectedAnswer.getAnswer());
  }

  private void logCachedProfiles(User user, List<GetUserProfileResponseDto> profiles) {
    profiles.forEach(profile -> log.info(
        "step=일일설문_추천_캐시_조회, requestUserId={}, recommendedUserId={}, age={}, gender={}, nickname={}, matchingScore={}, matchingStatus={}, hashtag={}, faceVerified={}, daysFromLastAccess={}, mbti={}",
        user.getId(), profile.getUserId(), profile.getAge(), profile.getGender(),
        profile.getNickname(), profile.getMatchingScore(), profile.getMatchingStatus(),
        profile.getHashtags(), profile.getVerify(), profile.getDaysFromLastAccess(), profile.getMbti()
    ));
  }
}
