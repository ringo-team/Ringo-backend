package com.lingo.lingoproject.matching.application;

import com.lingo.lingoproject.community.presentation.dto.GetPlaceDetailResponseDto;
import com.lingo.lingoproject.matching.domain.event.MatchingAcceptedEvent;
import com.lingo.lingoproject.matching.domain.event.MatchingRejectedEvent;
import com.lingo.lingoproject.matching.domain.event.MatchingRequestedEvent;
import com.lingo.lingoproject.matching.domain.service.SurveyScoreCalculator;
import com.lingo.lingoproject.matching.domain.service.MatchingValidationService;
import com.lingo.lingoproject.matching.domain.service.RecommendationDomainService;
import com.lingo.lingoproject.shared.domain.event.DomainEventPublisher;
import com.lingo.lingoproject.shared.domain.model.AnsweredSurvey;
import com.lingo.lingoproject.shared.domain.model.BlockedUser;
import com.lingo.lingoproject.shared.domain.model.DormantAccount;
import com.lingo.lingoproject.shared.domain.model.FaceVerify;
import com.lingo.lingoproject.shared.domain.model.Hashtag;
import com.lingo.lingoproject.shared.domain.model.Matching;
import com.lingo.lingoproject.shared.domain.model.MatchingStatus;
import com.lingo.lingoproject.shared.domain.model.Place;
import com.lingo.lingoproject.shared.domain.model.Profile;
import com.lingo.lingoproject.shared.domain.model.ScrappedUser;
import com.lingo.lingoproject.shared.domain.model.SignupStatus;
import com.lingo.lingoproject.shared.domain.model.Survey;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.domain.model.UserActivityLog;
import com.lingo.lingoproject.shared.domain.model.UserMatchingLog;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import com.lingo.lingoproject.shared.infrastructure.persistence.KeywordRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.UserScrapPlaceRepository;
import com.lingo.lingoproject.shared.utils.ApiListResponseDto;
import com.lingo.lingoproject.shared.utils.RedisUtils;
import com.lingo.lingoproject.matching.presentation.dto.GetScrappedUserResponseDto;
import com.lingo.lingoproject.matching.presentation.dto.GetUserProfileResponseDto;
import com.lingo.lingoproject.matching.presentation.dto.MatchingRequestDto;
import com.lingo.lingoproject.matching.presentation.dto.SaveMatchingRequestMessageRequestDto;
import com.lingo.lingoproject.shared.infrastructure.persistence.PlaceRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.MatchingRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.ScrappedUserRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.UserActivityLogRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.UserMatchingLogRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.AnsweredSurveyRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.SurveyRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.BlockedFriendRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.BlockedUserRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.DormantAccountRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.HashtagRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.ProfileRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.UserRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * 매칭 요청·응답, 추천 이성, 스크랩 등 매칭 도메인의 비즈니스 로직을 담당한다.
 *
 * <pre>매칭 상태 흐름: PRE_REQUESTED → (메시지 저장) → PENDING → ACCEPTED / REJECTED</pre>
 *
 * <p>추천 이성은 누적 설문({@link #getCumulativeSurveyBasedRecommendationProfiles})과
 * 일일 설문({@link #getDailySurveyBasedRecommendationProfiles}) 두 가지 방식을 제공한다.
 * 블락·휴면·정지·기매칭 유저는 {@link #getRecommendationExcludedUserIds}에서 일괄 제외된다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchService {

  private final UserRepository userRepository;
  private final MatchingRepository matchingRepository;
  private final BlockedFriendRepository blockedFriendRepository;
  private final DormantAccountRepository dormantAccountRepository;
  private final ProfileRepository profileRepository;
  private final AnsweredSurveyRepository answeredSurveyRepository;
  private final DomainEventPublisher eventPublisher;
  private final SurveyScoreCalculator surveyScoreCalculator;
  private final MatchingValidationService matchingValidationService;
  private final RecommendationDomainService recommendationDomainService;
  private final HashtagRepository hashtagRepository;
  private final BlockedUserRepository blockedUserRepository;
  private final RedisUtils redisUtils;
  private final UserMatchingLogRepository userMatchingLogRepository;
  private final RedisTemplate<String, Object> redisTemplate;
  private final SurveyRepository surveyRepository;
  private final PlaceRepository placeRepository;
  private final ScrappedUserRepository scrappedUserRepository;
  private final UserActivityLogRepository userActivityLogRepository;
  private final KeywordRepository keywordRepository;
  private final UserScrapPlaceRepository userScrapPlaceRepository;

  private static final int MAX_CUMULATIVE_SURVEY_BASED_RECOMMENDATION_SIZE = 4;
  private static final int MAX_DAILY_SURVEY_BASED_RECOMMENDATION_SIZE = 4;
  private static final float SURVEY_SCORE_THRESHOLD = 0;
  private static final int MAX_MATCHING_POOL_SIZE = 30;
  private static final int ACTIVE_DAY_DURATION = 14;
  private static final int HIDE_PROFILE_FLAG = 1;
  private static final int EXPOSE_PROFILE_FLAG = 0;
  private static final int PROFILE_VERIFICATION_FLAG = 1;
  private static final int PROFILE_NON_VERIFICATION_FLAG = 0;
  private static final String REDIS_ACTIVE_USER_IDS = "redis::active::ids";
  private static final String DAILY_RECOMMENDATION_REDIS_KEY_PREFIX = "recommend-for-daily-survey::";
  private static final String CUMULATIVE_RECOMMENDATION_REDIS_KEY_PREFIX = "recommend::";
  private static final List<Integer> PLACE_SELECTION_COUNT = List.of(7, 4, 3, 2, 1);
  private static final List<Integer> POSITIVE_ANSWER_LIST = List.of(3, 4, 5);

  // ============================================================
  // 매칭 요청 / 응답
  // ============================================================

  /** PENDING 상태로 매칭을 생성하고 알림 이벤트를 발행한다. */
  @Transactional
  public Matching requestMatching(MatchingRequestDto dto) {
    User requestUser = findUserOrThrow(dto.requestId());
    User requestedUser = findUserOrThrow(dto.requestedId());

    matchingValidationService.validateAlreadyMatched(requestUser, requestedUser);

    float surveyScore = surveyScoreCalculator.calculate(requestedUser.getId(), requestUser.getId());

    Matching matching = Matching.of(
        requestUser,
        requestedUser,
        surveyScore,
        MatchingStatus.PENDING,
        dto.message()
    );

    log.info("step=매칭_요청, requestUserId={}, requestedUserId={}, surveyScore={}",
        requestUser.getId(), requestedUser.getId(), surveyScore);

    Matching saved = matchingRepository.save(matching);
    UserMatchingLog matchingLog = saved.createMatchingLog();
    userMatchingLogRepository.save(matchingLog);

    eventPublisher.publish(new MatchingRequestedEvent(
        saved.getId(),
        requestUser.getId(),
        requestedUser.getId()
    ));
    return saved;
  }

  /** decision 값에 따라 ACCEPTED / REJECTED 상태로 변경하고 이벤트를 발행한다. */
  @Transactional
  public void respondToMatchingRequest(
      String decision,
      Long matchingId,
      User user
  ) {
    Matching matching = findMatchingOrThrow(matchingId);

    matchingValidationService.validateMatchingRespondPermission(matching, user);
    matchingValidationService.validateIsAlreadyDecidedMatching(matching);

    switch (decision) {
      case "ACCEPTED" -> acceptMatching(matching);
      case "REJECTED" -> rejectMatching(matching);
      default -> throw new RingoException(
          "decision 값이 적절하지 않습니다.",
          ErrorCode.BAD_PARAMETER,
          HttpStatus.BAD_REQUEST);
    }

    UserMatchingLog matchingLog = matching.createRespondLog(user);

    log.info("step=매칭_응답, requestedUserId={}, requestUserId={}, matchingStatus={}, matchingScore={}",
        user.getId(), matching.getRequestUser().getId(),
        matching.getMatchingStatus(), matching.getMatchingScore());

    userMatchingLogRepository.save(matchingLog);
    matchingRepository.save(matching);
  }

  /** 요청자·수락자 본인만 삭제할 수 있다. */
  @Transactional
  public void deleteMatching(Long matchingId, User user) {
    Matching match = findMatchingOrThrow(matchingId);

    log.info("step=매칭_삭제_요청, matchingId={}, requestorId={}", matchingId, user.getId());
    matchingValidationService.validateMatchingDeletePermission(match, user);
    matchingRepository.deleteById(matchingId);
  }

  /** 요청 메시지를 저장하고 상태를 PRE_REQUESTED → PENDING으로 전환한다. */
  @Transactional
  public void saveMatchingRequestMessage(
      SaveMatchingRequestMessageRequestDto dto,
      Long matchingId,
      User user
  ) {
    Matching matching = findMatchingOrThrow(matchingId);

    matchingValidationService.validateMatchingMessageWritePermission(matching, user);
    matchingValidationService.validateIsAlreadyDecidedMatching(matching);

    matching.updateRequestMessage(dto.message());

    User requestUser = matching.getRequestUser();
    UserMatchingLog matchingLog = matching.createMatchingLog();

    log.info("step=매칭_요청_메세지_저장, matchingId={}, requestUserId={}, matchingStatus={}, requestUserGender={}",
        matching.getId(), requestUser.getId(),
        matching.getMatchingStatus(), requestUser.getGender());

    matchingRepository.save(matching);
    userMatchingLogRepository.save(matchingLog);
  }

  /** 요청자·수락자 본인만 조회할 수 있다. */
  public String getMatchingRequestMessage(Long matchingId, User user) {
    Matching match = findMatchingOrThrow(matchingId);

    matchingValidationService.validateMatchingMessageReadPermission(match, user);
    return match.getMatchingRequestMessage();
  }

  /** 보낸 매칭 요청 프로필 목록 */
  public List<GetUserProfileResponseDto> getSentMatchingProfiles(User requestUser) {
    List<Long> matchingIds = extractMatchingIds(requestUser, matchingRepository::findAllByRequestUser);

    List<GetUserProfileResponseDto> profiles =
        profileRepository.getRequestedUserProfilesByMatchingIds(matchingIds);

    enrichProfilesWithUserInfo(profiles);
    return profiles;
  }

  /** 받은 매칭 요청 프로필 목록 */
  public List<GetUserProfileResponseDto> getReceivedMatchingProfiles(User requestedUser) {
    List<Long> matchingIds = extractMatchingIds(requestedUser, matchingRepository::findAllByRequestedUser);

    List<GetUserProfileResponseDto> profiles =
        profileRepository.getRequestUserProfilesByMatchingIds(matchingIds);

    enrichProfilesWithUserInfo(profiles);
    return profiles;
  }

  private void acceptMatching(Matching matching) {
    matching.accept();
    matchingRepository.save(matching);
    eventPublisher.publish(new MatchingAcceptedEvent(
        matching.getId(),
        matching.getRequestUser().getId(),
        matching.getRequestedUser().getId()
    ));
  }

  private void rejectMatching(Matching matching) {
    matching.reject();
    matchingRepository.save(matching);
    eventPublisher.publish(new MatchingRejectedEvent(
        matching.getId(),
        matching.getRequestUser().getId(),
        matching.getRequestedUser().getId()
    ));
  }

  private Matching findMatchingOrThrow(Long matchingId) {
    return matchingRepository.findById(matchingId)
        .orElseThrow(() -> new RingoException(
            "적절하지 않은 매칭 id 입니다.",
            ErrorCode.NOT_FOUND,
            HttpStatus.BAD_REQUEST));
  }

  private List<Long> extractMatchingIds(User user, Function<User, List<Matching>> function) {
    return function.apply(user)
        .stream()
        .filter(matching -> !matching.getMatchingStatus().equals(MatchingStatus.PRE_REQUESTED))
        .map(Matching::getId)
        .toList();
  }

  // ============================================================
  // 추천 이성
  // ============================================================

  /** 누적 설문 점수 기반 추천 이성 목록을 반환한다. 자정까지 Redis에 캐싱된다. */
  @Transactional
  public List<GetUserProfileResponseDto> getCumulativeSurveyBasedRecommendationProfiles(User user) {
    Long userId = user.getId();

    List<GetUserProfileResponseDto> cached = getCumulativeCachedProfile(user);
    if (cached != null && cached.size() == MAX_CUMULATIVE_SURVEY_BASED_RECOMMENDATION_SIZE) {
      log.info("step=누적설문_추천_캐시_히트, userId={}, cacheSize={}", userId, cached.size());
      return cached;
    }

    List<Long> activeUserIds = getActiveUserIds(user);
    List<UserScoreEntry> candidates = getUserAboveSurveyScore(userId, activeUserIds);
    List<Long> selectedIds = selectCandidatesByScore(candidates);
    log.info("step=누적설문_추천_대상_선정, selectedIds={}", selectedIds);

    List<GetUserProfileResponseDto> result = getUserProfilesByIds(user, selectedIds);

    redisUtils.cacheUntilMidnight(
        CUMULATIVE_RECOMMENDATION_REDIS_KEY_PREFIX + userId,
        new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), result)
    );
    return result;
  }

  /** 오늘 답변한 설문 기반 추천 이성을 반환한다. 오늘 답변이 없으면 null. */
  public List<GetUserProfileResponseDto> getDailySurveyBasedRecommendationProfiles(User user) {
    Long userId = user.getId();

    List<GetUserProfileResponseDto> cached = getDailyCachedProfile(user);
    if (cached != null && cached.size() == MAX_DAILY_SURVEY_BASED_RECOMMENDATION_SIZE) {
      return cached;
    }

    List<AnsweredSurvey> todayAnswers = getTodayAnsweredSurveys(user);
    if (todayAnswers.isEmpty()) {
      log.info("step=금일_설문_응답_부재, todayAnswersSize=0");
      return null;
    }

    Collections.shuffle(todayAnswers);

    List<Long> excludedUserIds = getRecommendationExcludedUserIds(user);
    log.info("step=일일설문_추천_제외유저_조회, excludedUserCount={}", excludedUserIds.size());

    List<GetUserProfileResponseDto> result = getSimilarAnsweredUserProfiles(
        user, todayAnswers, excludedUserIds
    );

    redisUtils.cacheUntilMidnight(
        DAILY_RECOMMENDATION_REDIS_KEY_PREFIX + userId,
        new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), result)
    );
    return result;
  }

  /** 누적·일일 추천 캐시 양쪽에서 해당 유저의 hide 플래그를 1로 설정한다. */
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

  /** 최근 14일 이내 활동 유저 ID를 Redis에 5분 TTL로 캐싱한다. */
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

  /** 본인·차단·휴면·정지·기매칭 유저를 추천 제외 대상으로 수집한다. */
  public List<Long> getRecommendationExcludedUserIds(User user) {
    Long userId = user.getId();

    List<Long> blockedFriendIds = blockedFriendRepository.findUsersMutuallyBlockedWith(userId);
    List<Long> dormantUserIds = getDormantUserIds();
    List<Long> blockedUserIds = getBlockedUserIds();
    List<Long> suspendedUserIds = getSuspendedUserIds();
    List<Long> usersWhoRequestedMe = getUserWhoRequestedMe(user);
    List<Long> usersIRequested = getUsersIRequested(user);

    Set<Long> excluded = new HashSet<>();
    excluded.add(userId);
    excluded.addAll(blockedFriendIds);
    excluded.addAll(dormantUserIds);
    excluded.addAll(blockedUserIds);
    excluded.addAll(suspendedUserIds);
    excluded.addAll(usersWhoRequestedMe);
    excluded.addAll(usersIRequested);

    log.info("step=추천_제외_유저_수집, excludedUserIds={}", excluded);
    return new ArrayList<>(excluded);
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

  private List<GetUserProfileResponseDto> getUserProfilesByIds(User user, List<Long> selectedIds) {
    return userRepository.findAllByIdIn(selectedIds)
        .stream()
        .map(recommended -> buildUserProfileDto(user, recommended))
        .toList();
  }

  private List<Long> selectCandidatesByScore(List<UserScoreEntry> candidates) {
    return candidates.stream()
        .map(entry -> {
          User user = findUserOrThrow(entry.userId());
          double finalScore = recommendationDomainService.calculateFinalMatchingScore(user, entry.score());
          return Map.entry(entry.userId(), finalScore);
        })
        .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
        .map(Map.Entry::getKey)
        .limit(MAX_CUMULATIVE_SURVEY_BASED_RECOMMENDATION_SIZE)
        .toList();
  }

  private List<UserScoreEntry> getUserAboveSurveyScore(Long userId, List<Long> activeUserIds) {
    return userRepository.findAllByIdIn(activeUserIds)
        .stream()
        .filter(u -> u.getStatus() == SignupStatus.COMPLETED)
        .map(User::getId)
        .map(id -> new UserScoreEntry(id, surveyScoreCalculator.calculate(userId, id)))
        .filter(entry -> entry.score() >= SURVEY_SCORE_THRESHOLD)
        .sorted((entry1, entry2) -> (int) (entry2.score() - entry1.score()))
        .limit(MAX_MATCHING_POOL_SIZE)
        .toList();
  }

  private List<GetUserProfileResponseDto> getSimilarAnsweredUserProfiles(
      User user,
      List<AnsweredSurvey> todayAnswers,
      List<Long> excludedUserIds
  ) {
    List<GetUserProfileResponseDto> result = new ArrayList<>();
    for (AnsweredSurvey todayAnswer : todayAnswers) {
      if (result.size() == MAX_DAILY_SURVEY_BASED_RECOMMENDATION_SIZE) break;

      User recommendedUser = getRandomlySimilarAnsweredUser(todayAnswer, excludedUserIds);
      if (recommendedUser == null) continue;

      result.add(buildUserProfileDto(user, recommendedUser));
    }
    return result;
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

  private List<GetUserProfileResponseDto> getCumulativeCachedProfile(User user) {
    return getCachedRecommendationProfiles(
        CUMULATIVE_RECOMMENDATION_REDIS_KEY_PREFIX,
        redisUtils::getCumulativeSurveyBasedCachedProfile,
        user
    );
  }

  private List<GetUserProfileResponseDto> getDailyCachedProfile(User user) {
    return getCachedRecommendationProfiles(
        DAILY_RECOMMENDATION_REDIS_KEY_PREFIX,
        redisUtils::getDailySurveyBasedCachedProfile,
        user
    );
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

  private List<Long> getUsersIRequested(User user) {
    return matchingRepository.findAllByRequestUser(user)
        .stream()
        .map(Matching::getRequestedUser)
        .map(User::getId)
        .toList();
  }

  private List<Long> getUserWhoRequestedMe(User user) {
    return matchingRepository.findAllByRequestedUser(user)
        .stream()
        .map(Matching::getRequestUser)
        .map(User::getId)
        .toList();
  }

  private List<Long> getSuspendedUserIds() {
    return redisTemplate.keys("suspension::*")
        .stream()
        .map(s -> s.replace("suspension::", ""))
        .map(Long::parseLong)
        .toList();
  }

  private List<Long> getBlockedUserIds() {
    return blockedUserRepository.findAll()
        .stream()
        .map(BlockedUser::getBlockedUserId)
        .toList();
  }

  private List<Long> getDormantUserIds() {
    return dormantAccountRepository.findAll()
        .stream()
        .map(DormantAccount::getUser)
        .map(User::getId)
        .toList();
  }

  // ============================================================
  // 추천 컨텐츠
  // ============================================================

  /** 동일 설문 응답 기반 매칭 이유를 최대 5개 반환한다. */
  @Transactional
  public List<String> getMatchReasons(Long user1, Long user2) {
    return findSortedRelatedAnswerPairs(user1, user2)
        .stream()
        .map(pair -> {
          Survey survey = findSurveyByIdOrThrow(pair.getSurveyId());
          return POSITIVE_ANSWER_LIST.contains(pair.getAnswer())
              ? survey.getMatchedReasonForHigherAnswer()
              : survey.getMatchedReasonForLowerAnswer();
        })
        .limit(5)
        .toList();
  }

  public List<GetPlaceDetailResponseDto> getMatchedUserPlaces(
      User user,
      Long user1,
      Long user2
  ) {
    List<String> keywords = getMatchedKeywords(user1, user2);

    List<Place> places = selectPlacesByKeywords(keywords);
    if (places.size() < 4) places = dealWithHasFewKeywords();

    return buildPlaceDetailInfo(places, user);
  }

  public List<GetPlaceDetailResponseDto> getIndividualUserPlaces(User user) {
    List<String> keywords = getIndividualSurveyBasedKeywords(user);

    List<Place> places = selectPlacesByKeywords(keywords);
    if (places.size() < 4) places = dealWithHasFewKeywords();

    return buildPlaceDetailInfo(places, user);
  }

  public List<GetPlaceDetailResponseDto> getRandomlySelectedPlaces(User user) {
    List<Place> places = placeRepository.findAll();
    Collections.shuffle(places);
    return buildPlaceDetailInfo(places.subList(0, 50), user);
  }

  public List<GetPlaceDetailResponseDto> getRankedPagedPlaces(
      User user,
      int page,
      int size
  ) {
    List<Place> places = placeRepository.findAll();

    // 추후 click count로 정렬
    List<Place> slicedPlaces = new ArrayList<>();
    if (page * size < places.size()) {
      slicedPlaces = places.subList(page * size, Math.min((page + 1) * size, places.size()));
    }
    return buildPlaceDetailInfo(slicedPlaces, user);
  }

  /** 두 유저의 공통 설문에서 추천 장소 키워드를 최대 5개 반환한다. */
  public List<String> getMatchedKeywords(Long user1, Long user2) {
    List<Integer> positiveAnswers = List.of(3, 4, 5);

    return findSortedRelatedAnswerPairs(user1, user2)
        .stream()
        .flatMap(pair -> {
          Survey survey = findSurveyByIdOrThrow(pair.getSurveyId());
          String keyword = positiveAnswers.contains(pair.getAnswer())
              ? survey.getKeywordForHigherAnswer().strip()
              : survey.getKeywordForLowerAnswer().strip();
          return tokenizeKeywords(keyword).stream()
              .map(s -> Map.entry(s, pair.getOrderWeight()));
        })
        .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.summingLong(Map.Entry::getValue)))
        .entrySet()
        .stream()
        .map(entry -> {
          int score = keywordRepository.findAllByKeywordContaining(entry.getKey());
          return Map.entry(entry.getKey(), score * entry.getValue());
        })
        .sorted((entry1, entry2) -> Math.toIntExact(entry2.getValue() - entry1.getValue()))
        .map(Map.Entry::getKey)
        .limit(5)
        .toList();
  }

  /** 개인 설문 응답 점수 기반 추천 장소 키워드를 반환한다. */
  public List<String> getIndividualSurveyBasedKeywords(User user) {
    return answeredSurveyRepository.findAllByUser(user)
        .stream()
        .map(answeredSurvey -> Map.entry(
            answeredSurvey,
            recommendationDomainService.calculateAnswerScore(answeredSurvey.getAnswer())
        ))
        .flatMap(entry -> {
          Survey survey = surveyRepository.findBySurveyNum(entry.getKey().getSurveyNum());
          String keyword = POSITIVE_ANSWER_LIST.contains(entry.getKey().getAnswer())
              ? survey.getKeywordForHigherAnswer()
              : survey.getKeywordForLowerAnswer();
          return tokenizeKeywords(keyword).stream()
              .map(key -> Map.entry(key, entry.getValue()));
        })
        .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.summingLong(Map.Entry::getValue)))
        .entrySet()
        .stream()
        .map(entry -> {
          int score = keywordRepository.findAllByKeywordContaining(entry.getKey());
          return Map.entry(entry.getKey(), score * entry.getValue());
        })
        .sorted((entry1, entry2) -> Math.toIntExact(entry2.getValue() - entry1.getValue()))
        .map(Map.Entry::getKey)
        .toList();
  }

  public List<GetPlaceDetailResponseDto> buildPlaceDetailInfo(List<Place> places, User user) {
    return places.stream()
        .map(place -> {
          boolean isScrap = userScrapPlaceRepository.existsByUserAndPlace(user, place);
          return place.createPlaceDetailDto(isScrap);
        })
        .toList();
  }

  private List<Place> selectPlacesByKeywords(List<String> keywords) {
    List<Place> result = new ArrayList<>();
    for (int index = 0; index < keywords.size(); index++) {
      List<Place> places = placeRepository.findAllByKeywordContainingIgnoreCase(keywords.get(index));
      int max = Math.min(PLACE_SELECTION_COUNT.get(index), places.size());
      Collections.shuffle(places);
      result.addAll(places.subList(0, max));
    }
    Collections.shuffle(result);
    return result;
  }

  private List<Place> dealWithHasFewKeywords() {
    List<Place> places = placeRepository.findAllByType("RINGO_PICK");
    Collections.shuffle(places);

    places.subList(0, 10);
    return places;
  }

  private List<SortedAnswerPairWithWeight> findSortedRelatedAnswerPairs(Long user1, Long user2) {
    return answeredSurveyRepository.getRelatedSurveyAnswerPairs(user1, user2)
        .stream()
        .filter(pair -> Math.abs(pair.getAnswer() - pair.getConfrontAnswer()) <= 2)
        .sorted((a, b) -> {
          int scoreA = recommendationDomainService.calculateAnswerPairScore(a.getAnswer(), a.getConfrontAnswer());
          int scoreB = recommendationDomainService.calculateAnswerPairScore(b.getAnswer(), a.getConfrontAnswer());
          return scoreB - scoreA;
        })
        .map(pair -> {
          int weight = recommendationDomainService.calculateAnswerPairScore(pair.getAnswer(), pair.getConfrontAnswer());
          return new SortedAnswerPairWithWeight(pair.getAnswer(), pair.getConfrontAnswer(), pair.getSurveyId(), weight);
        })
        .toList();
  }

  private Survey findSurveyByIdOrThrow(Long surveyId) {
    return surveyRepository.findById(surveyId)
        .orElseThrow(() -> new RingoException(
            "적절하지 않은 설문아이디입니다.",
            ErrorCode.INTERNAL_SERVER_ERROR,
            HttpStatus.INTERNAL_SERVER_ERROR));
  }

  // ============================================================
  // 스크랩
  // ============================================================

  /** 추천 목록에 있는 유저만 스크랩 가능. 이미 스크랩이면 취소. */
  @Transactional
  public void scrapUser(Long recommendedUserId, User user) {
    Long userId = user.getId();

    List<Long> cumulativeRecommendedIds = extractIdFromProfileDtos(getCumulativeCachedProfile(user));
    log.info("step=스크랩_누적설문_추천목록, recommendedIds={}", cumulativeRecommendedIds);

    List<Long> dailyRecommendedIds = extractIdFromProfileDtos(getDailyCachedProfile(user));
    log.info("step=스크랩_일일설문_추천목록, recommendedIds={}", dailyRecommendedIds);

    List<Long> allRecommendedIds = new ArrayList<>(cumulativeRecommendedIds);
    allRecommendedIds.addAll(dailyRecommendedIds);

    if (!allRecommendedIds.contains(recommendedUserId)) {
      log.info("step=스크랩_대상아님, requestUserId={}, targetUserId={}, recommendedIds={}",
          userId, recommendedUserId, allRecommendedIds);
      return;
    }

    User recommendedUser = findUserOrThrow(recommendedUserId);

    if (scrappedUserRepository.existsByUserAndScrappedUser(user, recommendedUser)) {
      scrappedUserRepository.deleteByUserAndScrappedUser(user, recommendedUser);
      return;
    }
    scrappedUserRepository.save(ScrappedUser.of(user, recommendedUser));
  }

  /** 스크랩한 추천 이성 목록 조회 */
  public List<GetScrappedUserResponseDto> getScrappedUsers(User user) {
    return scrappedUserRepository.findAllByUser(user)
        .stream()
        .map(ScrappedUser::getUser)
        .map(scrappedUser -> {
          Profile profile = scrappedUser.getProfile();
          return new GetScrappedUserResponseDto(
              user.getId(),
              user.getNickname(),
              LocalDate.now().getYear() - user.getBirthday().getYear(),
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

  // ============================================================
  // private helpers
  // ============================================================

  private User findUserOrThrow(Long userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new RingoException(
            "유저를 찾을 수 없습니다.",
            ErrorCode.NOT_FOUND_USER,
            HttpStatus.BAD_REQUEST));
  }

  /** 매칭 점수·해시태그·인증 여부 등을 포함한 프로필 DTO를 생성한다. */
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

    log.info("step=추천이성_프로필_빌드, recommendedUserId={}, age={}, gender={}, nickname={}, matchingScore={}, hashtags={}, faceVerified={}, daysFromLastAccess={}, mbti={}",
        dto.getUserId(), dto.getAge(), dto.getGender(), dto.getNickname(),
        dto.getMatchingScore(), dto.getHashtags(), dto.getVerify(),
        dto.getDaysFromLastAccess(), dto.getMbti());

    return dto;
  }

  /** 프로필 목록에 해시태그·나이 등 추가 사용자 정보를 채워넣는다. */
  private void enrichProfilesWithUserInfo(List<GetUserProfileResponseDto> profiles) {
    profiles.forEach(profile -> {
      User user = findUserOrThrow(profile.getUserId());

      profile.setHashtags(getUserHashtags(user));
      profile.setAge(LocalDate.now().getYear() - user.getBirthday().getYear());

      log.info("step=매칭_프로필_조회, userId={}, age={}, gender={}, nickname={}, matchingScore={}, matchingStatus={}, hashtags={}, faceVerified={}, daysFromLastAccess={}, mbti={}",
          profile.getUserId(), profile.getAge(), profile.getGender(),
          profile.getNickname(), profile.getMatchingScore(), profile.getMatchingStatus(),
          profile.getHashtags(), profile.getVerify(), profile.getDaysFromLastAccess(),
          profile.getMbti());
    });
  }

  private void updateIsScrapFlag(User user, List<GetUserProfileResponseDto> profiles, String keyPrefix) {
    for (GetUserProfileResponseDto profile : profiles) {
      User scrappedUser = findUserOrThrow(profile.getUserId());
      boolean isScrap = scrappedUserRepository.existsByUserAndScrappedUser(user, scrappedUser);
      profile.setScrap(isScrap);
    }
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

  /** Redis 캐시가 없거나 비어 있으면 빈 리스트를 반환한다. */
  @SuppressWarnings("unchecked")
  private List<Long> getCachedActiveUserIds() {
    Object cache = redisTemplate.opsForValue().get(REDIS_ACTIVE_USER_IDS);
    if (cache instanceof ApiListResponseDto<?> dto && dto.getList() != null) {
      return new ArrayList<>((List<Long>) dto.getList());
    }
    return new ArrayList<>();
  }

  private void logCachedProfiles(User user, List<GetUserProfileResponseDto> profiles) {
    profiles.forEach(profile -> log.info(
        "step=일일설문_추천_캐시_조회, requestUserId={}, recommendedUserId={}, age={}, gender={}, nickname={}, matchingScore={}, matchingStatus={}, hashtags={}, faceVerified={}, daysFromLastAccess={}, mbti={}",
        user.getId(), profile.getUserId(), profile.getAge(), profile.getGender(),
        profile.getNickname(), profile.getMatchingScore(), profile.getMatchingStatus(),
        profile.getHashtags(), profile.getVerify(), profile.getDaysFromLastAccess(), profile.getMbti()
    ));
  }

  private List<String> getUserHashtags(User user) {
    return toHashtagStrings(hashtagRepository.findAllByUser(user));
  }

  private List<String> toHashtagStrings(List<Hashtag> hashtags) {
    return hashtags.stream().map(Hashtag::getHashtag).toList();
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

  private List<String> tokenizeKeywords(String keywords) {
    return Arrays.stream(keywords.split(",")).toList();
  }

  @Getter
  @RequiredArgsConstructor
  public static class SortedAnswerPairWithWeight {
    private final int answer;
    private final int confront;
    private final Long surveyId;
    private final int orderWeight;
  }

  private record UserScoreEntry(Long userId, float score) {}
}
