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
import java.time.temporal.ChronoUnit;
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
 * 매칭 요청·응답, 추천 이성 조회, 스크랩 등 매칭 도메인 전반의 비즈니스 로직을 담당하는 서비스.
 *
 * <h2>매칭 상태 흐름</h2>
 * <pre>
 *   PRE_REQUESTED → (요청 메시지 저장) → PENDING → ACCEPTED / REJECTED
 * </pre>
 *
 * <h2>추천 이성 알고리즘</h2>
 * <ul>
 *   <li><b>누적 설문 기반</b>({@link #getCumulativeSurveyBasedRecommendationProfiles}):
 *       활성 유저 중 매칭 점수 임계값({@code MATCHING_SCORE_THRESHOLD}) 이상인 후보를
 *       Fisher-Yates 셔플로 최대 {@code MAX_CUMULATIVE_SURVEY_BASED_RECOMMENDATION_SIZE}명 추출.
 *       결과는 자정까지 Redis에 캐싱.</li>
 *   <li><b>일일 설문 기반</b>({@link #getDailySurveyBasedRecommendationProfiles}):
 *       오늘 답변한 설문과 ±1 범위의 설문번호에서 동일한 답을 고른 유저를 랜덤 추출.
 *       오늘 답변 이력이 없으면 null 반환.</li>
 * </ul>
 *
 * <h2>추천 제외 대상</h2>
 * <p>블락 친구, 휴면 계정, 이용정지 계정(Redis {@code suspension::userId}), 이미 매칭 이력이 있는 유저는
 * {@link #getRecommendationExcludedUserIds}에서 일괄 수집하여 후보 풀에서 제거합니다.</p>
 *
 * <h2>이벤트 발행</h2>
 * <p>매칭 요청 시 {@link MatchingRequestedEvent}, 수락 시 {@link MatchingAcceptedEvent}를 발행합니다.
 * {@link MatchingAcceptedEvent}를 수신한 핸들러가 채팅방을 자동 생성합니다.</p>
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

  private static final int MAX_CUMULATIVE_SURVEY_BASED_RECOMMENDATION_SIZE = 4;
  private static final int MAX_DAILY_SURVEY_BASED_RECOMMENDATION_SIZE = 4;
  private static final float SURVEY_SCORE_THRESHOLD = 0;
  private static final int MAX_MATCHING_POOL_SIZE = 30;
  private static final int ACTIVE_DAY_DURATION = 14;
  private static final int HIDE_PROFILE_FLAG = 1;
  private static final int EXPOSE_PROFILE_FLAG = 0;
  private static final int PROFILE_VERIFICATION_FLAG = 1;
  private static final int PROFILE_NON_VERIFICATION_FLAG = 0;
  private static final String REDIS_ACTIVE_USER_IDS = "redis:active:ids";
  private static final String DAILY_RECOMMENDATION_REDIS_KEY_PREFIX = "recommend-for-daily-survey::";
  private static final String CUMULATIVE_RECOMMENDATION_REDIS_KEY_PREFIX = "recommend::";
  private static final List<Integer> PLACE_SELECTION_COUNT = List.of(7, 4, 3, 2, 1);
  private static final List<Integer> POSITIVE_ANSWER_LIST = List.of(3, 4, 5);
  private final UserScrapPlaceRepository userScrapPlaceRepository;

  // ============================================================
  // 매칭 요청 / 응답
  // ============================================================

  /**
   * 매칭 요청을 생성하고 {@link MatchingRequestedEvent}를 발행한다.
   *
   * <p>매칭 점수를 계산한 뒤 {@link Matching} 엔티티를 저장하고, 이벤트 발행을 통해
   * 알림 전송 등의 후속 처리를 트리거합니다.
   * 요청 직후 상태는 {@link MatchingStatus#PRE_REQUESTED}이며,
   * 요청 메시지 저장 후 {@link MatchingStatus#PENDING}으로 전환됩니다.</p>
   *
   * @param dto 매칭 요청자 ID({@code requestId}), 매칭 대상자 ID({@code requestedId}) 포함
   * @return 저장된 Matching 엔티티
   * @throws RingoException 요청자 또는 대상자가 존재하지 않는 경우
   */
  @Transactional
  public Matching matchRequest(MatchingRequestDto dto) {
    User requestUser = findUserOrThrow(dto.requestId());
    User requestedUser = findUserOrThrow(dto.requestedId());

    float surveyScore = surveyScoreCalculator.calculate(requestedUser.getId(), requestUser.getId());

    Matching matching = Matching.of(requestUser, requestedUser, surveyScore, MatchingStatus.PRE_REQUESTED);

    log.info("step=매칭_요청, requestUserId={}, requestedUserId={}, surveyScore={}",
        requestUser.getId(), requestedUser.getId(), surveyScore);

    Matching saved = matchingRepository.save(matching);
    eventPublisher.publish(new MatchingRequestedEvent(
        saved.getId(),
        requestUser.getId(),
        requestedUser.getId()
    ));
    return saved;
  }

  /**
   * 매칭 수락 시 ACCEPTED 상태 변경 + 채팅방 생성 이벤트 발행
   * 거절 시 REJECTED 상태 변경
   */
  public void respondToMatchingRequest(String decision, Long matchingId, User user) {
    Matching matching = findMatchingOrThrow(matchingId);

    matchingValidationService.validateMatchingRespondPermission(matching, user);

    switch (decision) {
      case "ACCEPTED" -> acceptMatching(matching);
      case "REJECTED" -> rejectMatching(matching);
      default -> throw new RingoException("decision 값이 적절하지 않습니다.", ErrorCode.BAD_PARAMETER, HttpStatus.BAD_REQUEST);
    }

    UserMatchingLog matchingLog = matching.createRespondLog(user);

    log.info("step=매칭_응답, requestedUserId={}, requestUserId={}, matchingStatus={}, matchingScore={}",
        user.getId(), matching.getRequestUser().getId(),
        matching.getMatchingStatus(), matching.getMatchingScore());

    userMatchingLogRepository.save(matchingLog);
    matchingRepository.save(matching);
  }

  private Matching findMatchingOrThrow(Long matchingId){
    return matchingRepository.findById(matchingId)
        .orElseThrow(() -> new RingoException("적절하지 않은 매칭 id 입니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));
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

  // ============================================================
  // 추천 이성
  // ============================================================

  /**
   * 최근 {@code ACTIVE_DAY_DURATION}일 이내 활동한 사용자 ID 목록을 Redis에 갱신한다.
   *
   * <p>{@link UserActivityLog}를 기준으로 활성 유저를 수집하고,
   * {@code redis:active:ids} 키에 1시간 TTL로 저장합니다.
   * 추천 이성 풀을 구성할 때 이 캐시를 활용합니다.</p>
   */
  @Transactional
  public void refreshActiveUserCache() {
    LocalDateTime cutoff = LocalDate.now().minusDays(ACTIVE_DAY_DURATION).atStartOfDay();
    List<Long> activeUserIds = userActivityLogRepository.findAllByStartAfter(cutoff)
        .stream()
        .map(UserActivityLog::getUser)
        .map(User::getId)
        .distinct()
        .toList();
    redisTemplate.delete(REDIS_ACTIVE_USER_IDS);
    redisTemplate.opsForValue().set(
        REDIS_ACTIVE_USER_IDS,
        new ApiListResponseDto<>(ErrorCode.SUCCESS.toString(), activeUserIds),
        1,
        TimeUnit.HOURS
    );
  }

  /**
   * 누적 설문 점수 기반으로 추천 이성 목록을 반환한다.
   *
   * <p>Redis 캐시({@code recommend::{userId}})에 유효한 결과가 있으면 즉시 반환합니다.
   * 캐시가 없거나 부족하면 활성 유저 풀에서 제외 대상을 걸러낸 뒤,
   * 매칭 점수 임계값 이상인 후보를 Fisher-Yates 셔플로 최대
   * {@code MAX_CUMULATIVE_SURVEY_BASED_RECOMMENDATION_SIZE}명 선정합니다.
   * 결과는 자정까지 Redis에 캐싱됩니다.</p>
   *
   * @param user 추천을 요청하는 사용자
   * @return 추천 이성 프로필 목록
   */
  @Transactional
  public List<GetUserProfileResponseDto> getCumulativeSurveyBasedRecommendationProfiles(User user) {
    Long userId = user.getId();

    List<GetUserProfileResponseDto> cached = getCumulativeCachedProfile(user);
    if (cached != null) return cached;

    List<Long> activeUserIds = findActiveUserIds(user);

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

  @Transactional
  List<Long> findActiveUserIds(User user){
    List<Long> activeUserIds = getCachedActiveUserIds();
    if (activeUserIds.isEmpty()) {
      refreshActiveUserCache();
      activeUserIds = getCachedActiveUserIds();
    }
    log.info("step=누적설문_추천_활성유저_조회, activeUserCount={}", activeUserIds.size());

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

  private List<Long> getRandomlySelectedCandidate(List<Long> candidates){
    return recommendationDomainService.selectRandomCandidates(
        candidates, MAX_CUMULATIVE_SURVEY_BASED_RECOMMENDATION_SIZE);
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

  private List<UserScoreEntry> getUserAboveSurveyScore(Long userId, List<Long> activeUserIds){
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

  /**
   * 오늘 답변한 일일 설문을 기반으로 추천 이성 목록을 반환한다.
   *
   * <p>오늘 이미 답변 이력이 있으면 null을 반환합니다.
   * 오늘 답변한 설문과 동일한 답을 고른(설문번호 ±1 포함) 유저 중 제외 대상을 걸러낸 뒤
   * 랜덤으로 최대 {@code MAX_RECOMMENDATION_SIZE_FOR_DAILY_SURVEY}명을 선정합니다.
   * 결과는 자정까지 Redis에 캐싱됩니다.</p>
   *
   * @param user 추천을 요청하는 사용자
   * @return 추천 이성 프로필 목록, 오늘 답변 이력이 없으면 {@code null}
   */
  public List<GetUserProfileResponseDto> getDailySurveyBasedRecommendationProfiles(User user) {
    Long userId = user.getId();

    List<GetUserProfileResponseDto> cached = getDailyCacheProfile(user);
    if(cached != null) return cached;

    List<AnsweredSurvey> todayAnswers = getUserTodayDailyAnswer(user);
    if (todayAnswers.isEmpty()) {
      log.info("step=금일_설문_응답_부재, todayAnswersSize=0");
      return null;
    }

    Collections.shuffle(todayAnswers);

    List<Long> excludedUserIds = getRecommendationExcludedUserIds(user);
    log.info("step=일일설문_추천_제외유저_조회, excludedUserCount={}", excludedUserIds.size());

    List<GetUserProfileResponseDto> result = new ArrayList<>();
    for (AnsweredSurvey todayAnswer : todayAnswers) {
      if (result.size() == MAX_DAILY_SURVEY_BASED_RECOMMENDATION_SIZE) break;

      User recommendedUser = getRandomlySimilarAnsweredUser(todayAnswer, excludedUserIds);
      if (recommendedUser == null)  continue;

      result.add(buildUserProfileDto(user, recommendedUser));
    }

    if(result.size() < MAX_DAILY_SURVEY_BASED_RECOMMENDATION_SIZE)
      log.info("step=추천_이성_수_조회, recommendationProfileSize={}", result.size());

    redisUtils.cacheUntilMidnight(
        DAILY_RECOMMENDATION_REDIS_KEY_PREFIX + userId,
        new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), result)
    );
    return result;
  }

  private User getRandomlySimilarAnsweredUser(AnsweredSurvey todayAnswer, List<Long> excludedUserIds){

    List<AnsweredSurvey> similarAnswers = getSimilarAnswers(todayAnswer, excludedUserIds);

    if (similarAnswers.isEmpty()) return null;

    int index = ThreadLocalRandom.current().nextInt(similarAnswers.size());

    User recommendedUser = similarAnswers.get(index).getUser();

    logDailySurveyBasedRecommendationLogic(todayAnswer, similarAnswers.get(index), todayAnswer.getUser(), recommendedUser);

    return recommendedUser;
  }

  private List<GetUserProfileResponseDto> getDailyCacheProfile(User user){
    return getCachedRecommendationProfiles(
        DAILY_RECOMMENDATION_REDIS_KEY_PREFIX,
        MAX_DAILY_SURVEY_BASED_RECOMMENDATION_SIZE,
        redisUtils::getDailySurveyBasedCachedProfile,
        user
    );
  }

  private void logDailySurveyBasedRecommendationLogic(
      AnsweredSurvey todayAnswer,
      AnsweredSurvey selectedAnswer,
      User user,
      User recommended
  ){
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

  /**
   * 추천 이성 목록에서 특정 유저를 숨김 처리한다.
   *
   * <p>누적 설문 캐시와 일일 설문 캐시 양쪽 모두에서 해당 유저의 {@code hide} 플래그를 1로 설정합니다.
   * 실제 캐시 삭제가 아닌 플래그 변경 방식이므로 자정 캐시 만료 시 자동 초기화됩니다.</p>
   *
   * @param user              숨김을 요청한 사용자
   * @param recommendedUserId 숨김 처리할 추천 이성의 유저 ID
   */
  public void hideRecommendedUser(User user, Long recommendedUserId) {
    applyHideFlagToCache(CUMULATIVE_RECOMMENDATION_REDIS_KEY_PREFIX, user, recommendedUserId, this::getCumulativeCachedProfile);
    applyHideFlagToCache(DAILY_RECOMMENDATION_REDIS_KEY_PREFIX, user, recommendedUserId, this::getDailyCacheProfile);
  }

  /**
   * 추천 후보 풀에서 제외해야 할 유저 ID 목록을 수집한다.
   *
   * <p>다음 대상을 모두 합산하여 반환합니다.</p>
   * <ul>
   *   <li>요청자 본인</li>
   *   <li>상호 차단된 친구</li>
   *   <li>휴면 계정({@link DormantAccount})</li>
   *   <li>이용정지 계정(Redis {@code suspension::userId} 키 보유)</li>
   *   <li>나에게 매칭을 요청한 유저</li>
   *   <li>내가 매칭을 요청한 유저</li>
   * </ul>
   *
   * @param user 추천을 요청하는 사용자
   * @return 제외 대상 유저 ID 목록 (중복 없음)
   */
  public List<Long> getRecommendationExcludedUserIds(User user) {
    Long userId = user.getId();

    List<Long> blockedFriendIds = blockedFriendRepository.findUsersMutuallyBlockedWith(userId);

    List<Long> dormantUserIds = getDormantUserIds();

    List<Long> blockedUserIds = getBlockUserIds();

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

  private List<Long> getUsersIRequested(User user){
    return matchingRepository.findAllByRequestUser(user)
        .stream()
        .map(Matching::getRequestedUser)
        .map(User::getId)
        .toList();
  }

  private List<Long> getUserWhoRequestedMe(User user){
    return matchingRepository.findAllByRequestedUser(user)
        .stream()
        .map(Matching::getRequestUser)
        .map(User::getId)
        .toList();
  }

  private List<Long> getSuspendedUserIds(){
    return redisTemplate.keys("suspension::*")
        .stream()
        .map(s -> s.replace("suspension::", ""))
        .map(Long::parseLong)
        .toList();
  }

  private List<Long> getBlockUserIds(){
    return blockedUserRepository.findAll()
        .stream()
        .map(BlockedUser::getBlockedUserId)
        .toList();
  }

  private List<Long> getDormantUserIds(){
    return dormantAccountRepository.findAll()
        .stream()
        .map(DormantAccount::getUser)
        .map(User::getId)
        .toList();
  }

  // ============================================================
  // 보낸/받은 매칭 목록
  // ============================================================

  /**
   * 보낸 매칭 요청 프로필
   */
  public List<GetUserProfileResponseDto> getSentMatchingProfiles(User requestUser) {
    List<Long> matchingIds = getSentMatchingId(requestUser, matchingRepository::findAllByRequestUser);

    List<GetUserProfileResponseDto> profiles =
        profileRepository.getRequestedUserProfilesByMatchingIds(matchingIds);

    enrichProfilesWithUserInfo(profiles);
    return profiles;
  }

  /**
   * 받은 매칭 요청 프로필
   */
  public List<GetUserProfileResponseDto> getReceivedMatchingProfiles(User requestedUser) {
    List<Long> matchingIds = getSentMatchingId(requestedUser, matchingRepository::findAllByRequestedUser);

    List<GetUserProfileResponseDto> profiles =
        profileRepository.getRequestUserProfilesByMatchingIds(matchingIds);

    enrichProfilesWithUserInfo(profiles);
    return profiles;
  }

  private List<Long> getSentMatchingId(User user, Function<User, List<Matching>> function){
    return function.apply(user)
        .stream()
        .filter(matching -> !matching.getMatchingStatus().equals(MatchingStatus.PRE_REQUESTED) )
        .map(Matching::getId)
        .toList();
  }

  // ============================================================
  // 매칭 관리
  // ============================================================

  /**
   * 매칭을 삭제한다.
   *
   * <p>매칭의 요청자 또는 수락자 본인만 삭제할 수 있습니다.</p>
   *
   * @param matchingId 삭제할 매칭 ID
   * @param user       요청 사용자
   * @throws RingoException 매칭이 존재하지 않거나 권한이 없는 경우
   */
  @Transactional
  public void deleteMatching(Long matchingId, User user) {
    Matching match = findMatchingOrThrow(matchingId);

    log.info("step=매칭_삭제_요청, matchingId={}, requestorId={}", matchingId, user.getId());
    matchingValidationService.validateMatchingDeletePermission(match, user);
    matchingRepository.deleteById(matchingId);
  }

  /**
   * 매칭 요청 메시지를 저장하고 상태를 {@link MatchingStatus#PENDING}으로 변경한다.
   *
   * <p>매칭 요청자 본인만 메시지를 작성할 수 있으며, 호출 시 매칭 상태가
   * {@code PRE_REQUESTED → PENDING}으로 전환됩니다.
   * 메시지는 상대방이 매칭 요청을 확인할 때 함께 표시됩니다.</p>
   *
   * @param dto        저장할 요청 메시지가 담긴 DTO
   * @param matchingId 대상 매칭 ID
   * @param user       요청 사용자 (요청자 본인 검증에 사용)
   * @throws RingoException 매칭이 존재하지 않거나 요청자가 아닌 경우
   */
  @Transactional
  public void saveMatchingRequestMessage(SaveMatchingRequestMessageRequestDto dto, Long matchingId, User user) {
    Matching matching = findMatchingOrThrow(matchingId);

    matchingValidationService.validateMatchingMessageWritePermission(matching, user);

    matching.updateRequestMessage(dto.message());

    User requestUser = matching.getRequestUser();
    UserMatchingLog matchingLog = matching.createMatchingLog();

    log.info("step=매칭_요청_메세지_저장, matchingId={}, requestUserId={}, matchingStatus={}, requestUserGender={}",
        matching.getId(), requestUser.getId(),
        matching.getMatchingStatus(), requestUser.getGender());

    matchingRepository.save(matching);
    userMatchingLogRepository.save(matchingLog);
  }

  /**
   * 매칭 요청 메시지를 조회한다.
   *
   * <p>매칭의 요청자 또는 수락자 본인만 조회할 수 있습니다.</p>
   *
   * @param matchingId 조회할 매칭 ID
   * @param user       요청 사용자
   * @return 매칭 요청 메시지 문자열
   * @throws RingoException 매칭이 존재하지 않거나 권한이 없는 경우
   */
  public String getMatchingRequestMessage(Long matchingId, User user) {
    Matching match = findMatchingOrThrow(matchingId);

    matchingValidationService.validateMatchingMessageReadPermission(match, user);
    return match.getMatchingRequestMessage();
  }

  // ============================================================
  // 매칭 이유 / 추천 컨텐츠
  // ============================================================

  /**
   * 두 사용자가 매칭된 이유를 설문 응답 기반으로 최대 5개 반환한다.
   *
   * <p>두 사용자가 동일하게 답한 설문 쌍을 점수 순으로 정렬하고,
   * 높은 점수의 응답(3~5)이면 긍정 이유, 낮은 응답(1~2)이면 부정 이유 문구를 사용합니다.</p>
   *
   * @param user1 첫 번째 사용자 ID
   * @param user2 두 번째 사용자 ID
   * @return 매칭 이유 문자열 목록 (최대 5개)
   */
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

  public List<GetPlaceDetailResponseDto> getMatchedUserPlaces(User user, Long user1, Long user2){
    List<String> keywords = getMatchedKeywords(user1, user2);

    List<Place> places = selectPlacesByKeywords(keywords);
    if (places.size() < 4) places = dealWithHasFewKeywords();

    return buildPlaceDetailInfo(places, user);
  }

  public List<GetPlaceDetailResponseDto> getIndividualUserPlaces(User user){
    List<String> keywords = getIndividualSurveyBasedKeywords(user);

    List<Place> places = selectPlacesByKeywords(keywords);
    if (places.size() < 4) places = dealWithHasFewKeywords();

    return buildPlaceDetailInfo(places, user);
  }

  public List<GetPlaceDetailResponseDto> getRandomlySelectedPlaces(User user){
    List<Place> places = placeRepository.findAll();
    Collections.shuffle(places);
    return buildPlaceDetailInfo(places.subList(0, 50), user);
  }

  public List<GetPlaceDetailResponseDto> getRankedPagedPlaces(User user, int page, int size){
    List<Place> places = placeRepository.findAll();

    // 추후 click count로 정렬
    List<Place> slicedPlaces = new ArrayList<>();
    if (page * size < places.size()) slicedPlaces = places.subList(page * size, Math.min((page + 1) * size, places.size()));
    return buildPlaceDetailInfo(slicedPlaces, user);
  }

  public List<GetPlaceDetailResponseDto> buildPlaceDetailInfo(List<Place> places, User user){
    return places.stream()
        .map(place -> {
          boolean isScrap = userScrapPlaceRepository.existsByUserAndPlace(user, place);
          return place.createPlaceDetailDto(isScrap);
        })
        .toList();
  }

  private List<Place> dealWithHasFewKeywords(){
    List<Place> places = placeRepository.findAllByType("RINGO_PICK");
    Collections.shuffle(places);

    places.subList(0, 10);
    return places;
  }

  private List<Place> selectPlacesByKeywords(List<String> keywords){
    List<Place> result = new ArrayList<>();
    for(int index = 0; index < keywords.size(); index++) {
      List<Place> places = placeRepository.findAllByKeywordContainingIgnoreCase(keywords.get(index));
      int max = Math.min(PLACE_SELECTION_COUNT.get(index), places.size());
      Collections.shuffle(places);
      result.addAll(places.subList(0, max));
    }
    Collections.shuffle(result);
    return result;
  }

  /**
   * 두 사용자의 공통 설문 응답을 기반으로 추천 장소 목록을 반환한다.
   *
   * <p>공통 설문 쌍에서 도출된 키워드로 {@link Place}을 검색합니다.
   * 최대 1,000개 제한이 있으며, 중복 장소가 포함될 수 있습니다.</p>
   *
   * @param user1 첫 번째 사용자 ID
   * @param user2 두 번째 사용자 ID
   * @return 추천 장소 목록
   */
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

  private Survey findSurveyByIdOrThrow(Long surveyId){
    return surveyRepository.findById(surveyId)
        .orElseThrow(() -> new RingoException("적절하지 않은 설문아이디입니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR));
  }

  /**
   * 사용자 개인 설문 응답을 기반으로 추천 장소 목록을 반환한다.
   *
   * <p>응답 점수가 극단적일수록(1 또는 5) 우선순위가 높은 설문부터 키워드를 추출하고,
   * 최대 5개의 추천 장소를 반환합니다.</p>
   *
   * @param user 추천을 요청하는 사용자
   * @return 추천 장소 목록 (최대 5개)
   */
  public List<String> getIndividualSurveyBasedKeywords(User user) {

    return answeredSurveyRepository.findAllByUser(user)
        .stream()
        .map(answeredSurvey -> Map.entry(answeredSurvey, recommendationDomainService.calculateAnswerScore(answeredSurvey.getAnswer())))
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

  // ============================================================
  // 스크랩
  // ============================================================

  /**
   * 추천 이성을 스크랩(관심 저장)한다.
   *
   * <p>누적 설문 또는 일일 설문 추천 목록에 포함된 유저만 스크랩할 수 있습니다.
   * 추천 목록에 없는 유저는 조용히 무시됩니다(예외 미발생).</p>
   *
   * @param recommendedUserId 스크랩할 추천 이성의 유저 ID
   * @param user              스크랩을 요청하는 사용자
   * @throws RingoException 스크랩 대상 유저가 존재하지 않는 경우
   */
  public void scrapUser(Long recommendedUserId, User user) {
    Long userId = user.getId();

    List<Long> cumulativeRecommendedIds =
        extractIdFromProfileDtos(getCumulativeCachedProfile(user));

    log.info("step=스크랩_누적설문_추천목록, recommendedIds={}", cumulativeRecommendedIds);

    List<Long> dailyRecommendedIds =
        extractIdFromProfileDtos(getDailyCacheProfile(user));
    log.info("step=스크랩_일일설문_추천목록, recommendedIds={}", dailyRecommendedIds);

    List<Long> allRecommendedIds = new ArrayList<>();
    allRecommendedIds.addAll(cumulativeRecommendedIds);
    allRecommendedIds.addAll(dailyRecommendedIds);

    if (!allRecommendedIds.contains(recommendedUserId)) {
      log.info("step=스크랩_대상아님, requestUserId={}, targetUserId={}, recommendedIds={}",
          userId, recommendedUserId, allRecommendedIds);
      return;
    }

    User recommendedUser = findUserOrThrow(recommendedUserId);

    scrappedUserRepository.save(ScrappedUser.of(user, recommendedUser));
  }

  private List<GetUserProfileResponseDto> getCumulativeCachedProfile(User user){
    return getCachedRecommendationProfiles(
        CUMULATIVE_RECOMMENDATION_REDIS_KEY_PREFIX,
        MAX_CUMULATIVE_SURVEY_BASED_RECOMMENDATION_SIZE,
        redisUtils::getCumulativeSurveyBasedCachedProfile,
        user);
  }

  private List<Long> extractIdFromProfileDtos(List<GetUserProfileResponseDto> profiles){
    return Optional.ofNullable(profiles)
            .orElseGet(Collections::emptyList)
            .stream()
            .map(GetUserProfileResponseDto::getUserId)
            .toList();
  }

  /**
   * 스크랩한 추천 이성 목록을 조회한다.
   *
   * @param user 스크랩 목록을 조회할 사용자
   * @return 스크랩된 유저의 닉네임·프로필·나이·얼굴 인증 여부 목록
   */
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

  // ============================================================
  // private helpers
  // ============================================================

  private User findUserOrThrow(Long userId){
    return userRepository.findById(userId)
        .orElseThrow(() -> new RingoException("유저를 찾을 수 없습니다.", ErrorCode.NOT_FOUND_USER, HttpStatus.BAD_REQUEST));
  }

  private List<AnsweredSurvey> getSimilarAnswers(AnsweredSurvey todayAnswer, List<Long> excludedUserIds){
    int surveyNum = todayAnswer.getSurveyNum();
    return answeredSurveyRepository.findAllByUserIdNotInAndAnswerAndSurveyNumIn(
        excludedUserIds,
        todayAnswer.getAnswer(),
        List.of(surveyNum, surveyNum + 1, surveyNum - 1)
    );
  }

  private List<AnsweredSurvey> getUserTodayDailyAnswer(User user){
    return answeredSurveyRepository.findAllByUserAndUpdatedAtAfter(
        user, LocalDate.now().atStartOfDay()
    );
  }

  private List<GetUserProfileResponseDto> getCachedRecommendationProfiles(
      String keyPrefix,
      int maxRecommendationSize,
      Function<String, List<GetUserProfileResponseDto>> function,
      User user
  ){
    Long userId = user.getId();
    if (redisTemplate.hasKey(keyPrefix + userId)) {
      List<GetUserProfileResponseDto> cached = function.apply(userId.toString());

      logDailyCachedProfile(user, cached);

      if (cached.size() == maxRecommendationSize) return cached;
    }
    return null;
  }

  private void logDailyCachedProfile(User user, List<GetUserProfileResponseDto> profiles){
    profiles.forEach(profile -> log.info(
        "step=일일설문_추천_캐시_조회, requestUserId={}, recommendedUserId={}, age={}, gender={}, nickname={}, matchingScore={}, matchingStatus={}, hashtags={}, faceVerified={}, daysFromLastAccess={}, mbti={}",
        user.getId(), profile.getUserId(), profile.getAge(), profile.getGender(),
        profile.getNickname(), profile.getMatchingScore(), profile.getMatchingStatus(),
        profile.getHashtags(), profile.getVerify(), profile.getDaysFromLastAccess(), profile.getMbti()
    ));
  }

  /** Redis에서 활성 유저 ID 목록을 가져온다. 캐시가 없거나 비어 있으면 빈 리스트를 반환한다. */
  @SuppressWarnings("unchecked")
  private List<Long> getCachedActiveUserIds() {
    Object cache = redisTemplate.opsForValue().get(REDIS_ACTIVE_USER_IDS);
    if (cache instanceof ApiListResponseDto<?> dto && dto.getList() != null) {
      return new ArrayList<>((List<Long>) dto.getList());
    }
    return new ArrayList<>();
  }

  /**
   * 추천 이성의 프로필 응답 DTO를 생성한다.
   *
   * <p>매칭 점수, 해시태그, 프로필 인증 여부, 마지막 접속일로부터 경과 일수를 함께 포함합니다.</p>
   *
   * @param requestUser   추천을 요청한 사용자 (매칭 점수 기준)
   * @param recommendedUser 추천 대상 사용자
   * @return 프로필 정보가 담긴 DTO
   */
  private GetUserProfileResponseDto buildUserProfileDto(User requestUser, User recommendedUser) {
    Profile profile = recommendedUser.getProfile();

    float surveyScore = surveyScoreCalculator.calculate(requestUser.getId(), recommendedUser.getId());
    int isVerify = FaceVerify.PASS == profile.getFaceVerify() ? PROFILE_VERIFICATION_FLAG : PROFILE_NON_VERIFICATION_FLAG;
    int accessBefore = getUserAccessDaysBefore(recommendedUser);
    List<String> hashtags = findAllStringHashTagValueByUser(recommendedUser);

    GetUserProfileResponseDto dto = GetUserProfileResponseDto.of(
        recommendedUser, surveyScore, hashtags, isVerify,
        EXPOSE_PROFILE_FLAG, accessBefore, requestUser.getMbti());

    log.info("step=추천이성_프로필_빌드, recommendedUserId={}, age={}, gender={}, nickname={}, matchingScore={}, hashtags={}, faceVerified={}, daysFromLastAccess={}, mbti={}",
        dto.getUserId(), dto.getAge(), dto.getGender(), dto.getNickname(),
        dto.getMatchingScore(), dto.getHashtags(), dto.getVerify(),
        dto.getDaysFromLastAccess(), dto.getMbti());

    return dto;
  }

  private int getUserAccessDaysBefore(User user){
    UserActivityLog activityLog = userActivityLogRepository.findFirstByUserOrderByCreateAtDesc(user);
    return (int) ChronoUnit.DAYS.between(activityLog.getCreateAt(), LocalDateTime.now());
  }

  /** 지정된 Redis 키의 추천 목록에서 특정 유저의 hide 플래그를 설정한다. */
  private void applyHideFlagToCache(
      String redisKeyPrefix,
      User user,
      Long targetUserId,
      Function<User, List<GetUserProfileResponseDto>> function) {
    List<GetUserProfileResponseDto> profiles = function.apply(user);
    if (profiles == null) return;
    markProfileAsHidden(profiles, targetUserId, user.getId());
    redisUtils.cacheUntilMidnight(redisKeyPrefix + user.getId(), new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), profiles));
  }

  /** 프로필 목록에서 targetUserId에 해당하는 항목의 hide 플래그를 1로 설정한다. */
  private void markProfileAsHidden(List<GetUserProfileResponseDto> profiles, Long targetUserId, Long requestUserId) {
    for (GetUserProfileResponseDto profile : profiles) {
      if (profile.getUserId().equals(targetUserId)) {
        profile.setHide(HIDE_PROFILE_FLAG);
        log.info("step=추천이성_숨김, requestUserId={}, hiddenUserId={}", requestUserId, targetUserId);
        return;
      }
    }
  }

  /** 프로필 목록에 해시태그·나이 등 추가 사용자 정보를 채워넣는다. */
  private void enrichProfilesWithUserInfo(List<GetUserProfileResponseDto> profiles) {
    profiles.forEach(profile -> {
      User user = findUserOrThrow(profile.getUserId());

      profile.setHashtags(findAllStringHashTagValueByUser(user));
      profile.setAge(LocalDate.now().getYear() - user.getBirthday().getYear());

      log.info("step=매칭_프로필_조회, userId={}, age={}, gender={}, nickname={}, matchingScore={}, matchingStatus={}, hashtags={}, faceVerified={}, daysFromLastAccess={}, mbti={}",
          profile.getUserId(), profile.getAge(), profile.getGender(),
          profile.getNickname(), profile.getMatchingScore(), profile.getMatchingStatus(),
          profile.getHashtags(), profile.getVerify(), profile.getDaysFromLastAccess(),
          profile.getMbti());
    });
  }

  private List<String> findAllStringHashTagValueByUser(User user){
    return extractStringValueFromHashTagEntity(hashtagRepository.findAllByUser(user));
  }

  private List<String> extractStringValueFromHashTagEntity(List<Hashtag> hashtags){
    return hashtags.stream().map(Hashtag::getHashtag).toList();
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

  @Getter
  public static class SortedAnswerPairWithWeight{
    private final int answer;
    private final int confront;
    private final Long surveyId;
    private final int orderWeight;
    public SortedAnswerPairWithWeight(int answer, int confront, Long surveyId, int orderWeight){
      this.answer = answer;
      this.confront = confront;
      this.surveyId = surveyId;
      this.orderWeight = orderWeight;
    }
  }

  private List<String> tokenizeKeywords(String keywords){
    return Arrays.stream(keywords.split(",")).toList();
  }

  private record UserScoreEntry(Long userId, float score) {}
}
