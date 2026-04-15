package com.lingo.lingoproject.matching.application;

import com.lingo.lingoproject.matching.domain.event.MatchingAcceptedEvent;
import com.lingo.lingoproject.matching.domain.event.MatchingRequestedEvent;
import com.lingo.lingoproject.matching.domain.service.MatchScoreCalculator;
import com.lingo.lingoproject.shared.domain.model.AnsweredSurvey;
import com.lingo.lingoproject.shared.domain.model.BlockedUser;
import com.lingo.lingoproject.shared.domain.model.DormantAccount;
import com.lingo.lingoproject.shared.domain.model.Hashtag;
import com.lingo.lingoproject.shared.domain.model.InspectStatus;
import com.lingo.lingoproject.shared.domain.model.Matching;
import com.lingo.lingoproject.shared.domain.model.MatchingStatus;
import com.lingo.lingoproject.shared.domain.model.Profile;
import com.lingo.lingoproject.shared.domain.model.Recommendation;
import com.lingo.lingoproject.shared.domain.model.ScrappedUser;
import com.lingo.lingoproject.shared.domain.model.SignupStatus;
import com.lingo.lingoproject.shared.domain.model.Survey;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.domain.model.UserAccessLog;
import com.lingo.lingoproject.shared.domain.model.UserActivityLog;
import com.lingo.lingoproject.shared.domain.model.UserMatchingLog;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import com.lingo.lingoproject.shared.utils.ApiListResponseDto;
import com.lingo.lingoproject.shared.utils.RedisUtils;
import com.lingo.lingoproject.matching.presentation.dto.GetScrappedUserResponseDto;
import com.lingo.lingoproject.matching.presentation.dto.GetUserProfileResponseDto;
import com.lingo.lingoproject.matching.presentation.dto.MatchScoreResultInterface;
import com.lingo.lingoproject.matching.presentation.dto.MatchingRequestDto;
import com.lingo.lingoproject.matching.presentation.dto.RelatedSurveyAnswerPairInterface;
import com.lingo.lingoproject.matching.presentation.dto.SaveMatchingRequestMessageRequestDto;
import com.lingo.lingoproject.shared.infrastructure.persistence.RecommendationRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.MatchingRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.ScrappedUserRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.UserActivityLogRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.UserMatchingLogRepository;
import com.lingo.lingoproject.notification.application.FcmNotificationUseCase;
import com.lingo.lingoproject.shared.infrastructure.persistence.AnsweredSurveyRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.SurveyRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.BlockedFriendRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.BlockedUserRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.DormantAccountRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.HashtagRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.ProfileRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.UserAccessLogRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.UserRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
 *   <li><b>누적 설문 기반</b>({@link #getRecommendationsByCumulativeSurvey}):
 *       활성 유저 중 매칭 점수 임계값({@code MATCHING_SCORE_THRESHOLD}) 이상인 후보를
 *       Fisher-Yates 셔플로 최대 {@code MAX_CUMULATIVE_SURVEY_BASED_RECOMMENDATION_SIZE}명 추출.
 *       결과는 자정까지 Redis에 캐싱.</li>
 *   <li><b>일일 설문 기반</b>({@link #getRecommendationsByDailySurvey}):
 *       오늘 답변한 설문과 ±1 범위의 설문번호에서 동일한 답을 고른 유저를 랜덤 추출.
 *       오늘 답변 이력이 없으면 null 반환.</li>
 * </ul>
 *
 * <h2>추천 제외 대상</h2>
 * <p>블락 친구, 휴면 계정, 이용정지 계정(Redis {@code suspension::userId}), 이미 매칭 이력이 있는 유저는
 * {@link #getExcludedUserIdsForRecommendation}에서 일괄 수집하여 후보 풀에서 제거합니다.</p>
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
  private final ApplicationEventPublisher eventPublisher;
  private final MatchScoreCalculator matchScoreCalculator;
  private final HashtagRepository hashtagRepository;
  private final BlockedUserRepository blockedUserRepository;
  private final RedisUtils redisUtils;
  private final FcmNotificationUseCase fcmService;
  private final UserMatchingLogRepository userMatchingLogRepository;
  private final UserAccessLogRepository userAccessLogRepository;
  private final RedisTemplate<String, Object> redisTemplate;
  private final SurveyRepository surveyRepository;
  private final RecommendationRepository recommendationRepository;
  private final ScrappedUserRepository scrappedUserRepository;
  private final UserActivityLogRepository userActivityLogRepository;

  private static final int MAX_CUMULATIVE_SURVEY_BASED_RECOMMENDATION_SIZE = 4;
  private static final int MAX_RECOMMENDATION_SIZE_FOR_DAILY_SURVEY = 4;
  private static final float MATCHING_SCORE_THRESHOLD = 0;
  private static final int ACTIVE_DAY_DURATION = 14;
  private static final int HIDE_PROFILE_FLAG = 1;
  private static final int EXPOSE_PROFILE_FLAG = 0;
  private static final int PROFILE_VERIFICATION_FLAG = 1;
  private static final int PROFILE_NON_VERIFICATION_FLAG = 0;
  private static final String REDIS_ACTIVE_USER_IDS = "redis:active:ids";
  private static final String DAILY_RECOMMENDATION_REDIS_KEY_PREFIX = "recommend-for-daily-survey::";
  private static final String CUMULATIVE_RECOMMENDATION_REDIS_KEY_PREFIX = "recommend::";

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
    User requestUser = userRepository.findById(dto.requestId())
        .orElseThrow(() -> new RingoException(
            "매칭을 요청한 유저를 찾을 수 없습니다.", ErrorCode.NOT_FOUND_USER, HttpStatus.BAD_REQUEST));
    User requestedUser = userRepository.findById(dto.requestedId())
        .orElseThrow(() -> new RingoException(
            "매칭을 요청 받은 유저를 찾을 수 없습니다.", ErrorCode.NOT_FOUND_USER, HttpStatus.BAD_REQUEST));

    float matchingScore = matchScoreCalculator.calculate(requestedUser.getId(), requestUser.getId());

    Matching matching = Matching.of(requestUser, requestedUser, matchingScore);

    log.info("step=매칭_요청, requestUserId={}, requestedUserId={}, matchingScore={}",
        requestUser.getId(), requestedUser.getId(), matchingScore);

    Matching saved = matchingRepository.save(matching);
    eventPublisher.publishEvent(new MatchingRequestedEvent(
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
    Matching matching = matchingRepository.findById(matchingId)
        .orElseThrow(() -> new RingoException("적절하지 않은 매칭 id 입니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));

    Long requestedUserId = matching.getRequestedUser().getId();
    if (!requestedUserId.equals(user.getId())) {
      log.error("step=잘못된_유저_요청, authUserId={}, matchRequestedUserId={}, status=FAILED",
          user.getId(), requestedUserId);
      throw new RingoException("매칭 수락 여부를 결정할 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
    }

    switch (decision) {
      case "ACCEPTED" -> acceptMatching(matching);
      case "REJECTED" -> matching.setMatchingStatus(MatchingStatus.REJECTED);
      default -> throw new RingoException("decision 값이 적절하지 않습니다.", ErrorCode.BAD_PARAMETER, HttpStatus.BAD_REQUEST);
    }

    UserMatchingLog matchingLog = UserMatchingLog.of(user.getId(), matchingId,
        matching.getMatchingStatus(), user.getGender());

    log.info("step=매칭_응답, requestedUserId={}, requestUserId={}, matchingStatus={}, matchingScore={}",
        user.getId(), requestedUserId,
        matching.getMatchingStatus(), matching.getMatchingScore());

    userMatchingLogRepository.save(matchingLog);
    matchingRepository.save(matching);
  }

  private void acceptMatching(Matching matching) {
    matching.setMatchingStatus(MatchingStatus.ACCEPTED);
    matchingRepository.save(matching);
    eventPublisher.publishEvent(new MatchingAcceptedEvent(
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
  public List<GetUserProfileResponseDto> getRecommendationsByCumulativeSurvey(User user) {
    Long userId = user.getId();

    if (redisTemplate.hasKey(CUMULATIVE_RECOMMENDATION_REDIS_KEY_PREFIX + userId)) {
      List<GetUserProfileResponseDto> cached = redisUtils.getRecommendedUserForCumulativeSurvey(userId.toString());
      if (cached != null && cached.size() == MAX_CUMULATIVE_SURVEY_BASED_RECOMMENDATION_SIZE) return cached;
    }

    List<Long> activeUserIds = getCachedActiveUserIds();
    if (activeUserIds.isEmpty()) {
      refreshActiveUserCache();
      activeUserIds = getCachedActiveUserIds();
    }
    log.info("step=누적설문_추천_활성유저_조회, activeUserCount={}", activeUserIds.size());

    List<Long> excludedUserIds = getExcludedUserIdsForRecommendation(user);
    log.info("step=누적설문_추천_제외유저_조회, excludedUserCount={}", excludedUserIds.size());

    activeUserIds.removeAll(excludedUserIds);
    log.info("step=누적설문_추천_풀_확정, candidateCount={}", activeUserIds.size());

    List<Long> candidates = userRepository.findAllByIdIn(activeUserIds)
        .stream()
        .filter(u -> u.getStatus() == SignupStatus.COMPLETED)
        .map(User::getId)
        .map(id -> new UserScoreEntry(id, matchScoreCalculator.calculate(userId, id)))
        .filter(entry -> entry.score() >= MATCHING_SCORE_THRESHOLD)
        .map(UserScoreEntry::userId)
        .collect(Collectors.toList());

    // Fisher-Yates shuffle 로 앞 n 명 추출
    int n = Math.min(candidates.size(), MAX_CUMULATIVE_SURVEY_BASED_RECOMMENDATION_SIZE);
    ThreadLocalRandom random = ThreadLocalRandom.current();
    for (int i = 0; i < n; i++) {
      int j = random.nextInt(i, candidates.size());
      Collections.swap(candidates, i, j);
    }
    List<Long> selectedIds = candidates.subList(0, n);
    log.info("step=누적설문_추천_대상_선정, selectedIds={}", selectedIds);

    Set<GetUserProfileResponseDto> profileSet = new HashSet<>();
    userRepository.findAllByIdIn(selectedIds)
        .forEach(recommended ->
            profileSet.add(buildUserProfileDto(user, recommended))
        );
    List<GetUserProfileResponseDto> result = new ArrayList<>(profileSet);

    redisUtils.cacheUntilMidnight(
        CUMULATIVE_RECOMMENDATION_REDIS_KEY_PREFIX + userId,
        new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), result)
    );
    return result;
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
  public List<GetUserProfileResponseDto> getRecommendationsByDailySurvey(User user) {
    Long userId = user.getId();

    if (redisTemplate.hasKey(DAILY_RECOMMENDATION_REDIS_KEY_PREFIX + userId)) {
      List<GetUserProfileResponseDto> cached = redisUtils.getRecommendUserForDailySurvey(userId.toString());
      cached.forEach(profile -> log.info(
          "step=일일설문_추천_캐시_조회, requestUserId={}, recommendedUserId={}, age={}, gender={}, nickname={}, matchingScore={}, matchingStatus={}, hashtags={}, faceVerified={}, daysFromLastAccess={}, mbti={}",
          user.getId(), profile.getUserId(), profile.getAge(), profile.getGender(),
          profile.getNickname(), profile.getMatchingScore(), profile.getMatchingStatus(),
          profile.getHashtags(), profile.getVerify(), profile.getDaysFromLastAccess(), profile.getMbti()
      ));
      if (cached.size() == MAX_RECOMMENDATION_SIZE_FOR_DAILY_SURVEY) return cached;
    }

    List<AnsweredSurvey> todayAnswers = answeredSurveyRepository.findAllByUserAndUpdatedAtAfter(
        user, LocalDate.now().atStartOfDay()
    );
    if (todayAnswers.isEmpty()) {
      return null;
    }

    Collections.shuffle(todayAnswers);

    List<Long> excludedUserIds = getExcludedUserIdsForRecommendation(user);
    log.info("step=일일설문_추천_제외유저_조회, excludedUserCount={}", excludedUserIds.size());

    List<GetUserProfileResponseDto> result = new ArrayList<>();
    for (AnsweredSurvey todayAnswer : todayAnswers) {
      if (result.size() == MAX_RECOMMENDATION_SIZE_FOR_DAILY_SURVEY) break;

      int surveyNum = todayAnswer.getSurveyNum();
      List<AnsweredSurvey> similarAnswers = answeredSurveyRepository.findAllByUserIdNotInAndAnswerAndSurveyNumIn(
          excludedUserIds,
          todayAnswer.getAnswer(),
          List.of(surveyNum, surveyNum + 1, surveyNum - 1)
      );

      if (similarAnswers.isEmpty()) {
        log.info("step=일일설문_추천_매칭없음, surveyNum={}", surveyNum);
        continue;
      }

      int idx = ThreadLocalRandom.current().nextInt(similarAnswers.size());
      User recommended = similarAnswers.get(idx).getUser();

      log.info("step=일일설문_추천_매칭, similarAnswerCount={}, selectedSurveyId={}, surveyNum={}, requestUserId={}, recommendedUserId={}, requestUserAnswer={}, recommendedUserAnswer={}",
          similarAnswers.size(),
          similarAnswers.get(idx).getId(), similarAnswers.get(idx).getSurveyNum(),
          user.getId(), recommended.getId(),
          todayAnswer.getAnswer(), similarAnswers.get(idx).getAnswer());

      result.add(buildUserProfileDto(user, recommended));
    }

    redisUtils.cacheUntilMidnight(
        DAILY_RECOMMENDATION_REDIS_KEY_PREFIX + userId,
        new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), result)
    );
    return result;
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
    applyHideFlagToCache(CUMULATIVE_RECOMMENDATION_REDIS_KEY_PREFIX, user.getId(), recommendedUserId);
    applyHideFlagToCache(DAILY_RECOMMENDATION_REDIS_KEY_PREFIX, user.getId(), recommendedUserId);
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
  public List<Long> getExcludedUserIdsForRecommendation(User user) {
    Long userId = user.getId();

    List<Long> blockedFriendIds = blockedFriendRepository.findUsersMutuallyBlockedWith(userId);

    List<Long> dormantUserIds = dormantAccountRepository.findAll()
        .stream()
        .map(DormantAccount::getUser)
        .map(User::getId)
        .toList();

    List<Long> blockedUserIds = blockedUserRepository.findAll()
        .stream()
        .map(BlockedUser::getBlockedUserId)
        .toList();

    List<Long> suspendedUserIds = redisTemplate.keys("suspension::*")
        .stream()
        .map(s -> s.replace("suspension::", ""))
        .map(Long::parseLong)
        .toList();

    List<Long> usersWhoRequestedMe = matchingRepository.findAllByRequestedUser(user)
        .stream()
        .map(Matching::getRequestUser)
        .map(User::getId)
        .toList();

    List<Long> usersIRequested = matchingRepository.findAllByRequestUser(user)
        .stream()
        .map(Matching::getRequestedUser)
        .map(User::getId)
        .toList();

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

  // ============================================================
  // 보낸/받은 매칭 목록
  // ============================================================

  /**
   * 내가 매칭 요청을 보낸 상대방 프로필 목록
   */
  public List<GetUserProfileResponseDto> getSentMatchingProfiles(User requestUser) {
    List<Long> matchingIds = matchingRepository.findAllByRequestUser(requestUser)
        .stream()
        .map(Matching::getId)
        .toList();

    List<GetUserProfileResponseDto> profiles =
        profileRepository.getRequestedUserProfilesByMatchingIds(matchingIds);

    enrichProfilesWithUserInfo(profiles);
    return profiles;
  }

  /**
   * 나에게 매칭 요청을 보낸 상대방 프로필 목록
   */
  public List<GetUserProfileResponseDto> getReceivedMatchingProfiles(User requestedUser) {
    List<Long> matchingIds = matchingRepository.findAllByRequestedUser(requestedUser)
        .stream()
        .filter(m -> m.getMatchingStatus() != MatchingStatus.PRE_REQUESTED)
        .map(Matching::getId)
        .toList();

    List<GetUserProfileResponseDto> profiles =
        profileRepository.getRequestUserProfilesByMatchingIds(matchingIds);

    enrichProfilesWithUserInfo(profiles);
    return profiles;
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
    Matching match = matchingRepository.findById(matchingId)
        .orElseThrow(() -> new RingoException("해당 매칭을 찾을 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));

    Long requestedUserId = match.getRequestedUser().getId();
    Long requestUserId = match.getRequestUser().getId();

    log.info("step=매칭_삭제_요청, matchingId={}, requestorId={}", matchingId, user.getId());

    if (!(requestedUserId.equals(user.getId()) || requestUserId.equals(user.getId()))) {
      log.error("step=잘못된_유저_요청, authUserId={}, matchRequestedUserId={}, status=FAILED",
          user.getId(), requestedUserId);
      throw new RingoException("매칭을 삭제할 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.BAD_REQUEST);
    }

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
    Matching matching = matchingRepository.findById(matchingId)
        .orElseThrow(() -> new RingoException("해당 매칭을 찾을 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));

    User requestUser = matching.getRequestUser();
    if (!requestUser.getId().equals(user.getId())) {
      log.error("step=잘못된_유저_요청, authUserId={}, matchRequestUserId={}, status=FAILED",
          user.getId(), requestUser.getId());
      throw new RingoException("요청 메세지를 저장 및 수정할 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.BAD_REQUEST);
    }

    matching.setMatchingStatus(MatchingStatus.PENDING);
    matching.setMatchingRequestMessage(dto.message());

    UserMatchingLog matchingLog = UserMatchingLog.of(requestUser.getId(), matching.getId(),
        MatchingStatus.PENDING, requestUser.getGender());

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
    Matching match = matchingRepository.findById(matchingId)
        .orElseThrow(() -> new RingoException("해당 매칭을 찾을 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));

    Long requestedUserId = match.getRequestedUser().getId();
    Long requestUserId = match.getRequestUser().getId();
    if (!(requestedUserId.equals(user.getId()) || requestUserId.equals(user.getId()))) {
      log.error("step=잘못된_유저_요청, authUserId={}, status=FAILED", user.getId());
      throw new RingoException("해당 매칭의 요청 메세지를 확인할 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.BAD_REQUEST);
    }

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
    List<Integer> positiveAnswers = List.of(3, 4, 5);

    return findSortedRelatedAnswerPairs(user1, user2)
        .stream()
        .map(pair -> {
          Survey survey = surveyRepository.findById(pair.getSurveyId())
              .orElseThrow(() -> new RingoException("적절하지 않은 설문아이디입니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR));
          return positiveAnswers.contains(pair.getAnswer())
              ? survey.getMatchedReasonForHigherAnswer()
              : survey.getMatchedReasonForLowerAnswer();
        })
        .limit(5)
        .toList();
  }

  /**
   * 두 사용자의 공통 설문 응답을 기반으로 추천 장소 목록을 반환한다.
   *
   * <p>공통 설문 쌍에서 도출된 키워드로 {@link Recommendation}을 검색합니다.
   * 최대 1,000개 제한이 있으며, 중복 장소가 포함될 수 있습니다.</p>
   *
   * @param user1 첫 번째 사용자 ID
   * @param user2 두 번째 사용자 ID
   * @return 추천 장소 목록
   */
  public List<Recommendation> getRecommendationsForMatchedPreference(Long user1, Long user2) {
    List<Integer> positiveAnswers = List.of(3, 4, 5);

    return findSortedRelatedAnswerPairs(user1, user2)
        .stream()
        .map(pair -> {
          Survey survey = surveyRepository.findById(pair.getSurveyId())
              .orElseThrow(() -> new RingoException("설문을 찾을 수 없습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR));
          String keyword = positiveAnswers.contains(pair.getAnswer())
              ? survey.getKeywordForHigherAnswer()
              : survey.getKeywordForLowerAnswer();
          return findRecommendationsByKeywords(keyword);
        })
        .flatMap(Collection::stream)
        .limit(1000)
        .toList();
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
  public List<Recommendation> getRecommendationsForIndividualSurvey(User user) {
    return answeredSurveyRepository.findAllByUser(user)
        .stream()
        .sorted((a, b) -> calculateAnswerScore(b.getAnswer()) - calculateAnswerScore(a.getAnswer()))
        .map(answeredSurvey -> {
          Survey survey = surveyRepository.findBySurveyNum(answeredSurvey.getSurveyNum());
          String keyword = List.of(3, 4, 5).contains(answeredSurvey.getAnswer())
              ? survey.getKeywordForHigherAnswer()
              : survey.getKeywordForLowerAnswer();
          return findRecommendationsByKeywords(keyword);
        })
        .flatMap(Collection::stream)
        .limit(5)
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
        Optional.ofNullable(redisUtils.getRecommendedUserForCumulativeSurvey(userId.toString()))
            .orElseGet(Collections::emptyList)
            .stream()
            .map(GetUserProfileResponseDto::getUserId)
            .toList();
    log.info("step=스크랩_누적설문_추천목록, recommendedIds={}", cumulativeRecommendedIds);

    List<Long> dailyRecommendedIds =
        Optional.ofNullable(redisUtils.getRecommendUserForDailySurvey(userId.toString()))
            .orElseGet(Collections::emptyList)
            .stream()
            .map(GetUserProfileResponseDto::getUserId)
            .toList();
    log.info("step=스크랩_일일설문_추천목록, recommendedIds={}", dailyRecommendedIds);

    List<Long> allRecommendedIds = new ArrayList<>();
    allRecommendedIds.addAll(cumulativeRecommendedIds);
    allRecommendedIds.addAll(dailyRecommendedIds);

    if (!allRecommendedIds.contains(recommendedUserId)) {
      log.info("step=스크랩_대상아님, requestUserId={}, targetUserId={}, recommendedIds={}",
          userId, recommendedUserId, allRecommendedIds);
      return;
    }

    User recommendedUser = userRepository.findById(recommendedUserId)
        .orElseThrow(() -> new RingoException("유저를 스크랩하던 도중 유저를 찾을 수 없습니다.", ErrorCode.NOT_FOUND_USER, HttpStatus.BAD_REQUEST));

    scrappedUserRepository.save(ScrappedUser.of(user, recommendedUser));
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
              InspectStatus.PASS == profile.getInspectStatus() ? 1 : 0
          );
        })
        .toList();
  }

  // ============================================================
  // private helpers
  // ============================================================

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

    List<String> hashtags = hashtagRepository.findAllByUser(recommendedUser)
        .stream()
        .map(Hashtag::getHashtag)
        .toList();

    UserAccessLog access = userAccessLogRepository.findFirstByUserIdOrderByCreateAtDesc(recommendedUser.getId());

    GetUserProfileResponseDto dto = GetUserProfileResponseDto.of(
        recommendedUser,
        matchScoreCalculator.calculate(requestUser.getId(), recommendedUser.getId()),
        hashtags,
        InspectStatus.PASS == profile.getInspectStatus() ? PROFILE_VERIFICATION_FLAG : PROFILE_NON_VERIFICATION_FLAG,
        EXPOSE_PROFILE_FLAG,
        (int) ChronoUnit.DAYS.between(access.getCreateAt(), LocalDateTime.now()),
        requestUser.getMbti());

    log.info("step=추천이성_프로필_빌드, recommendedUserId={}, age={}, gender={}, nickname={}, matchingScore={}, hashtags={}, faceVerified={}, daysFromLastAccess={}, mbti={}",
        dto.getUserId(), dto.getAge(), dto.getGender(), dto.getNickname(),
        dto.getMatchingScore(), dto.getHashtags(), dto.getVerify(),
        dto.getDaysFromLastAccess(), dto.getMbti());

    return dto;
  }

  /** 지정된 Redis 키의 추천 목록에서 특정 유저의 hide 플래그를 설정한다. */
  private void applyHideFlagToCache(String redisKeyPrefix, Long userId, Long targetUserId) {
    if (!redisTemplate.hasKey(redisKeyPrefix + userId)) return;
    List<GetUserProfileResponseDto> profiles = redisUtils.getRecommendedUserForCumulativeSurvey(userId.toString());
    if (profiles == null) return;
    markProfileAsHidden(profiles, targetUserId, userId);
    redisUtils.cacheUntilMidnight(redisKeyPrefix + userId, new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), profiles));
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
      User user = userRepository.findById(profile.getUserId()).orElse(null);
      if (user == null) return;
      profile.setHashtags(
          hashtagRepository.findAllByUser(user).stream()
              .map(Hashtag::getHashtag)
              .toList()
      );
      profile.setAge(LocalDate.now().getYear() - user.getBirthday().getYear());
      log.info("step=매칭_프로필_조회, userId={}, age={}, gender={}, nickname={}, matchingScore={}, matchingStatus={}, hashtags={}, faceVerified={}, daysFromLastAccess={}, mbti={}",
          profile.getUserId(), profile.getAge(), profile.getGender(),
          profile.getNickname(), profile.getMatchingScore(), profile.getMatchingStatus(),
          profile.getHashtags(), profile.getVerify(), profile.getDaysFromLastAccess(),
          profile.getMbti());
    });
  }

  /**
   * 두 사용자의 공통 설문 응답 쌍을 점수 순으로 정렬하여 반환한다.
   *
   * <p>두 응답의 차이가 2 이하인 쌍만 포함하고,
   * {@link #calculateAnswerPairScore}로 계산된 점수 내림차순으로 정렬합니다.</p>
   */
  private List<RelatedSurveyAnswerPairInterface> findSortedRelatedAnswerPairs(Long user1, Long user2) {
    return answeredSurveyRepository.getRelatedSurveyAnswerPairs(user1, user2)
        .stream()
        .filter(pair -> Math.abs(pair.getAnswer() - pair.getConfrontAnswer()) <= 2)
        .sorted((a, b) -> {
          int scoreA = calculateAnswerPairScore(a.getAnswer(), a.getConfrontAnswer());
          int scoreB = calculateAnswerPairScore(b.getAnswer(), a.getConfrontAnswer());
          return scoreB - scoreA;
        })
        .toList();
  }

  /*
   * 쌍문항에서 같은 응답을 할수록 그리고 극단적인 응답을 할수록
   * 높은 점수를 부여하여 매칭에 원인이 되는 문항이 될 확률이 높아지도록 함
   *
   * (5, 5) -> 15, (5, 4) -> 12, (5, 3) -> 5
   * (4, 4) -> 13, (4, 3) -> 10, (4, 2) -> 3
   * (3, 3) -> 10, (3, 1) -> 5
   * (2, 2) -> 13, (2, 1) -> 12
   * (1, 1) -> 15
   */
  private int calculateAnswerPairScore(int answer, int confrontAnswer) {
    int score = 0;
    if (Math.abs(answer - confrontAnswer) == 0) score += 10;
    else if (Math.abs(answer - confrontAnswer) == 1) score += 7;

    if ((answer == 5 || answer == 1) || (confrontAnswer == 5 || confrontAnswer == 1)) score += 5;
    else if ((answer == 4 || answer == 2) || (confrontAnswer == 4 || confrontAnswer == 2)) score += 3;

    return score;
  }

  /*
   * 1, 5 처럼 극단적인 값에 더 높은 점수를 부여
   */
  private int calculateAnswerScore(int answer) {
    if (answer == 1 || answer == 5) return 1;
    else if (answer == 2 || answer == 4) return 0;
    else return -1;
  }

  private List<Recommendation> findRecommendationsByKeywords(String keywords) {
    List<Recommendation> result = new ArrayList<>();
    Arrays.stream(keywords.split(","))
        .forEach(keyword ->
            result.addAll(recommendationRepository.findAllByKeywordContainingIgnoreCase(keyword))
        );
    return result;
  }

  private record UserScoreEntry(Long userId, float score) {}
}
