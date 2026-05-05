package com.lingo.lingoproject.matching.domain.service;

import com.lingo.lingoproject.matching.application.MatchService;
import com.lingo.lingoproject.matching.application.MatchService.UserScoreEntry;
import com.lingo.lingoproject.shared.domain.model.Profile;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.domain.model.UserActivityLog;
import com.lingo.lingoproject.shared.infrastructure.persistence.ProfileRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.UserActivityLogRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;


/**
 * 추천 이성 알고리즘의 도메인 로직을 담당하는 Domain Service.
 *
 * <p>추천 후보 랜덤 선정, 설문 응답 점수 계산 등
 * 순수 비즈니스 규칙을 캡슐화한다. 인프라(DB/Redis) 의존이 없다.</p>
 */
@Service
public class RecommendationDomainService {

  private static final int SURVEY_SCORE_RATIO = 60;
  private static final int ATTRACTION_SCORE_RATIO = 5;
  private static final int ACTIVITY_SCORE_RATIO = 10;
  private static final int EXPOSURE_BALANCE_SCORE_RATIO = 15;
  private static final int RECENT_SIGNUP_SCORE_RATIO = 10;
  private final ProfileRepository profileRepository;
  private final UserActivityLogRepository userActivityLogRepository;
  private final RedisTemplate<String, Object> redisTemplate;

  public RecommendationDomainService(ProfileRepository profileRepository,
      UserActivityLogRepository userActivityLogRepository,
      RedisTemplate<String, Object> redisTemplate) {
    this.profileRepository = profileRepository;
    this.userActivityLogRepository = userActivityLogRepository;
    this.redisTemplate = redisTemplate;
  }

  /**
   * Fisher-Yates 셔플로 후보 목록에서 최대 {@code maxSize}명을 랜덤 선정한다.
   *
   * <p>원본 리스트를 직접 수정하므로 호출 전에 방어적 복사가 필요하면
   * 호출자가 직접 처리해야 한다.</p>
   *
   * @param candidates 전체 후보 ID 목록 (변경됨)
   * @param maxSize    최대 선정 인원
   * @return 랜덤 선정된 후보 ID 목록 (candidates의 앞부분 서브리스트)
   */
  public List<Long> selectRandomCandidates(List<Long> candidates, int maxSize) {
    int n = Math.min(candidates.size(), maxSize);
    ThreadLocalRandom random = ThreadLocalRandom.current();
    for (int i = 0; i < n; i++) {
      int j = random.nextInt(i, candidates.size());
      Collections.swap(candidates, i, j);
    }
    return candidates.subList(0, n);
  }

  /**
   * 최종 매칭 점수를 산정한다 (0~100점 척도).
   *
   * <ul>
   *   <li>설문 점수 (60%): matchingScore(0~1) × 60</li>
   *   <li>어트랙션 점수 (5%): CTR 퍼센타일 랭크 (상대적 매력도)</li>
   *   <li>활동 점수 (10%): 오늘 접속 시간 지수 CDF 정규화 (λ=30분)</li>
   *   <li>노출 균형 점수 (15%): 노출 역퍼센타일 랭크 (적게 노출될수록 높음)</li>
   *   <li>신규 가입 부스트 (10%): 로지스틱 감쇠 (중심 30일, 2주 이내 ≈ 만점)</li>
   * </ul>
   */
  public Map<Long, Double> calculateFinalMatchingScore(Map<Long, Float> scoreMap) {
    List<Profile> profiles = profileRepository.findProfileByUserIdIn(scoreMap.keySet());
    Map<Long, Profile> profileMap = profiles.stream()
        .collect(Collectors.toMap(e -> e.getUser().getId(), Function.identity()));
    Map<Long, User> userMap = profiles.stream().map(Profile::getUser)
        .collect(Collectors.toMap(User::getId, Function.identity()));
    Map<Long, Double> profileCTRMap = profiles.stream()
        .collect(Collectors.toMap(e -> e.getUser().getId(), this::computeCTR));


    // findAll()을 한 번만 호출해 CTR·노출 분포를 공유한다
    List<Profile> allProfiles = profileRepository.findAll();
    List<Double> allCTRs = allProfiles.stream().map(this::computeCTR).toList();

    return scoreMap.keySet().stream()
        .map(userId -> {
          if (userMap.get(userId) == null) return null;
          if (profileMap.get(userId) == null) return null;

          double weightedSurveyScore = scoreMap.getOrDefault(userId, 0f) * SURVEY_SCORE_RATIO;
          double attractionScore = ATTRACTION_SCORE_RATIO * ctrPercentileRank(profileCTRMap.getOrDefault(userId, 0.0), allCTRs);
          double activityScore = ACTIVITY_SCORE_RATIO * normalizeActivityTime(getActivityTime(userMap.get(userId)));
          double exposureBalanceScore = EXPOSURE_BALANCE_SCORE_RATIO * impressionInverseRank(profileMap.get(userId).getImpressionCount(), allProfiles);
          double recentSignupScore = RECENT_SIGNUP_SCORE_RATIO * calcRecentSignupBoost(userMap.get(userId).getCreatedAt());

          double score = weightedSurveyScore + attractionScore + activityScore + exposureBalanceScore + recentSignupScore;
          return Map.entry(userId, score);
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }

  /**
   * 프로필의 CTR(클릭률)을 계산한다.
   *
   * <p>Laplace 스무딩(impressionCount + 1)으로 노출 0회인 신규 프로필의
   * 극단적 CTR(0 또는 ∞)을 방지하고 사전 확률(prior) 방향으로 수축시킨다.</p>
   */
  private double computeCTR(Profile profile) {
    return (double) profile.getClickCount() / (profile.getImpressionCount() + 1);
  }

  /**
   * 지수 CDF(누적 분포 함수)로 오늘 접속 시간을 0~1로 정규화한다.
   *
   * <p>λ=30분 기준: 30분 → 0.63, 60분 → 0.86, 120분 → 0.98.
   * 고정 상한 없이 시간이 늘수록 1에 수렴한다.</p>
   */
  private double normalizeActivityTime(int activityMinutes) {
    return 1.0 - Math.exp(-activityMinutes / 30.0);
  }

  /**
   * 전체 프로필 대비 CTR 퍼센타일 랭크를 반환한다 (0~1).
   *
   * <p>자신보다 CTR이 낮은 프로필의 비율이므로 CTR이 높을수록 높은 점수를 받는다.</p>
   */
  private double ctrPercentileRank(double ctr, List<Double> allCTRs) {
    if (allCTRs.isEmpty()) return 0.5;
    long below = allCTRs.stream().filter(c -> c < ctr).count();
    return (double) below / allCTRs.size();
  }

  /**
   * 노출 수 역퍼센타일 랭크를 반환한다 (0~1).
   *
   * <p>자신보다 노출이 많은 프로필의 비율이므로,
   * 노출이 적을수록 높은 점수를 받아 추천 빈도를 높인다.</p>
   */
  private double impressionInverseRank(int impressionCount, List<Profile> allProfiles) {
    if (allProfiles.isEmpty()) return 1.0;
    long above = allProfiles.stream().filter(p -> p.getImpressionCount() > impressionCount).count();
    return (double) above / allProfiles.size();
  }

  /**
   * 로지스틱 감쇠로 신규 가입 부스트를 계산한다 (0~1).
   *
   * <p>중심(midpoint) 30일, 기울기(k) 0.15 기준:
   * 0~14일 ≈ 0.99, 30일 = 0.50, 60일 이후 ≈ 0.01.
   * 순수 지수 감쇠와 달리 초기 구간을 일정하게 유지한 뒤 급격히 감소한다.</p>
   */
  private double calcRecentSignupBoost(LocalDateTime createdAt) {
    if (createdAt == null) return 0;
    long days = ChronoUnit.DAYS.between(createdAt, LocalDateTime.now());
    return 1.0 / (1.0 + Math.exp(0.15 * (days - 30)));
  }


  private int getActivityTime(User user) {
    return todayLoggedMinutes(user) + currentSessionMinutes(user);
  }

  private int todayLoggedMinutes(User user) {
    return userActivityLogRepository
        .findByUserAndCreateAtAfter(user, LocalDate.now().atStartOfDay())
        .stream()
        .mapToInt(UserActivityLog::getActivityMinuteDuration)
        .sum();
  }

  private int currentSessionMinutes(User user) {
    return redisTemplate.keys("connect-app::" + user.getId() + "*")
        .stream()
        .findFirst()
        .map(key -> key.split("::")[2])
        .map(LocalDateTime::parse)
        .map(connectedAt -> (int) ChronoUnit.MINUTES.between(connectedAt, LocalDateTime.now()))
        .orElse(0);
  }


  /**
   * 두 유저의 설문 응답 쌍에 대한 점수를 계산한다.
   *
   * <p>응답 차이가 적을수록, 극단값(1 또는 5)일수록 높은 점수를 부여한다.</p>
   * <pre>
   *   (5,5) → 5, (5,4) → 3, (5,3) → 1
   *   (4,4) → 4, (4,3) → 2, (4,2) → 0
   *   (3,3) → 4, (3,2) → 2  (3,1) → 1
   *   (2,2) → 4  (2,1) → 3
   *   (1,1) → 5
   * </pre>
   *
   * @param answer         한 유저의 응답 (1~5)
   * @param confrontAnswer 상대 유저의 응답 (1~5)
   * @return 계산된 매칭 점수
   */
  public int calculateAnswerPairScore(int answer, int confrontAnswer) {
    int score = 0;

    if (Math.abs(answer - confrontAnswer) == 0)      score += 4;
    else if (Math.abs(answer - confrontAnswer) == 1) score += 2;

    if ((answer == 5 || answer == 1) || (confrontAnswer == 5 || confrontAnswer == 1)) score += 1;

    return score;
  }

  /**
   * 개인 설문 응답의 극단성 점수를 계산한다.
   *
   * <p>추천 장소 우선순위 결정에 사용한다.
   * 1 또는 5처럼 극단적인 값일수록 더 높은 점수를 부여한다.</p>
   *
   * @param answer 응답값 (1~5)
   * @return 1(극단값), 0(준극단값), -1(중간값)
   */
  public int calculateAnswerScore(int answer) {
    if (answer == 1 || answer == 5)      return 5;
    else if (answer == 2 || answer == 4) return 2;
    else                                 return 0;
  }
}