package com.lingo.lingoproject.matching.application;

import com.lingo.lingoproject.matching.domain.event.UserProfileClickEvent;
import com.lingo.lingoproject.matching.domain.service.RecommendationDomainService;
import com.lingo.lingoproject.matching.domain.service.SurveyScoreCalculator;
import com.lingo.lingoproject.matching.presentation.dto.GetUserProfileResponseDto;
import com.lingo.lingoproject.shared.domain.event.DomainEventPublisher;
import com.lingo.lingoproject.shared.domain.model.AnsweredSurvey;
import com.lingo.lingoproject.shared.domain.model.FaceVerify;
import com.lingo.lingoproject.shared.domain.model.Hashtag;
import com.lingo.lingoproject.shared.domain.model.Profile;
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
import com.lingo.lingoproject.shared.utils.RedisKey;
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
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
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

  private static final int 누적_설문_기반_추천_크기 = 4;
  private static final int 일일_설문_기반_추천_크기 = 4;
  private static final float 설문_매칭_점수_임계값 = 0;
  private static final int 활성_유저_최대_최근_접속일 = 14;
  private static final int 프로필_숨김처리_활성화 = 1;
  private static final int 프로필_숨김처리_비활성화 = 0;
  private static final int 프로필_얼굴인증_활성화 = 1;
  private static final int 프로필_얼굴인증_비활성화 = 0;
  private final DomainEventPublisher domainEventPublisher;

  @Transactional
  public List<GetUserProfileResponseDto> 누적_설문_기반_추천(User user) {
    Long userId = user.getId();

    List<Long> cached = 캐시된_누적_설문_기반_추천_프로필_조회(user);
    if (!cached.isEmpty() && cached.size() == 누적_설문_기반_추천_크기) {
      log.info("step=누적설문_추천_캐시_히트, userId={}, cacheSize={}", userId, cached.size());
      return 다수_추천_프로필_생성(user, cached);
    }

    List<GetUserProfileResponseDto> result = 처음_접속한_유저를_위한_추천(user);

    redisUtils.cacheUntilMidnight(
        RedisKey.누적_설문_기반_추천_레디스_키 + userId,
        new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), result)
    );

    return result;
  }

  @Scheduled(cron = "0 40 0 * * *")
  public void 누적_설문_기반_추천_캐싱_스케줄링() {
    log.info("[누적 설문 스케줄링 시작]");
    List<User> users = userQueryUseCase.findAll();
    users.forEach(this::각각의_유저_누적_설문_기반_추천);
  }

  @Transactional
  void 각각의_유저_누적_설문_기반_추천(User user) {
    Long userId = user.getId();

    List<Long> 활성_유저_ids = 활성_유저_조회(user);
    Map<Long, Float> 후보 = 일정_설문_점수_이상의_추천_후보_조회(userId, 활성_유저_ids);
    List<Long> 선택된_유저_ids = 최종_매칭_점수_계산후_이성_추천(후보);
    log.info("step=누적설문_추천_대상_선정, selectedIds={}", 선택된_유저_ids);

    redisUtils.cacheUntilMidnight(
        RedisKey.누적_설문_기반_추천_레디스_키 + userId,
        new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), 선택된_유저_ids)
    );
  }

  @Transactional
  List<GetUserProfileResponseDto> 처음_접속한_유저를_위한_추천(User user) {

    List<Long> 활성_유저 = 활성_유저_조회(user);
    List<Long> 랜덤_유저_ids = recommendationDomainService.랜덤_유저_선택(활성_유저, 누적_설문_기반_추천_크기);

    return 다수_추천_프로필_생성(user, 랜덤_유저_ids);
  }

  @Transactional
  public List<GetUserProfileResponseDto> 일일_설문_기반_유저_추천(User user) {
    List<Long> cached = 캐시된_일일_설문_기반_추천_프로필_조회(user);
    if (!cached.isEmpty() && cached.size() == 일일_설문_기반_추천_크기) {
      return 다수_추천_프로필_생성(user, cached);
    }
    return 일일_설문_기반_추천_및_캐싱(user);
  }

  @Scheduled(cron = "0 50 0 * * *")
  public void 일일_설문_기반_추천_캐싱_스케줄링() {
    log.info("[일일 설문 스케줄링 시작]");
    List<User> users = userQueryUseCase.findAll();
    users.forEach(this::일일_설문_기반_추천_및_캐싱);
  }

  @Transactional
  List<GetUserProfileResponseDto> 일일_설문_기반_추천_및_캐싱(User user) {
    List<GetUserProfileResponseDto> result = 유저_일일_추천_기반_프로필_추천(user);

    if (result == null) return null;

    List<Long> idlist = result.stream().map(GetUserProfileResponseDto::getUserId).toList();
    redisUtils.cacheUntilMidnight(
        RedisKey.일일_설문_기반_추천_레디스_키 + user.getId(),
        new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), idlist)
    );

    return result;
  }

  public void hideRecommendedUser(User user, Long recommendedUserId) {
    프로필_숨김(
        RedisKey.누적_설문_기반_추천_레디스_키,
        user,
        recommendedUserId,
        this::캐시된_누적_설문_기반_추천_프로필_조회
    );
    프로필_숨김(
        RedisKey.일일_설문_기반_추천_레디스_키,
        user,
        recommendedUserId,
        this::캐시된_일일_설문_기반_추천_프로필_조회
    );
  }

  @Transactional
  public void 활성_유저_캐시_리프레시() {
    LocalDateTime cutoff = LocalDate.now().minusDays(활성_유저_최대_최근_접속일).atStartOfDay();
    Set<Long> 최근_접속한_유저_ids = userActivityLogRepository.findAllByStartAfter(cutoff)
        .stream()
        .map(UserActivityLog::getUserId)
        .collect(Collectors.toSet());
    ScanOptions options = ScanOptions.scanOptions()
        .match(RedisKey.접속_유저_레디스_키 + "*")
        .count(100)
        .build();
    Set<Long> 현재_접속중인_유저_ids = new HashSet<>();
    try (Cursor<String> cursor = redisTemplate.scan(options)){
      while (cursor.hasNext()) {
        String key = cursor.next();
        현재_접속중인_유저_ids.add(Long.parseLong(key.split("::")[1]));
      }
    }
    최근_접속한_유저_ids.addAll(현재_접속중인_유저_ids);
    List<Long> 모든_활성_유저_id_리스트 = 최근_접속한_유저_ids.stream().toList();
    redisTemplate.delete(RedisKey.활성_유저_레디스_키);
    redisTemplate.opsForValue().set(
        RedisKey.활성_유저_레디스_키,
        new ApiListResponseDto<>(ErrorCode.SUCCESS.toString(), 모든_활성_유저_id_리스트),
        5,
        TimeUnit.MINUTES
    );
  }

  public List<Long> 이성_추천에_제외되는_유저_조회(User user) {
    Long userId = user.getId();

    List<Long> blockedFriendIds    = 연락처_차단_유저_ids(userId);
    List<Long> dormantUserIds      = 휴면_계정_ids();
    List<Long> blockedUserIds      = 블락된_유저_ids();
    List<Long> suspendedUserIds    = 정지된_유저_ids();
    List<Long> usersWhoRequestedMe = 나에게_요청_보낸_유저_ids(user);
    List<Long> usersIRequested     = 내가_요청_보낸_유저_ids(user);
    List<Long> signupIncompleteUserIds = 회원가입_미완료_유저_ids();

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

  public List<Long> 연락처_차단_유저_ids(Long userId){
    return blockedFriendRepository.findUsersMutuallyBlockedWith(userId);
  }

  @Transactional
  public void updateProfileClickCount(Long userId, User user) {
    User profileUser = userQueryUseCase.유저_찾기_혹은_오류(userId);
    Profile profile = profileUser != null ? profileUser.getProfile() : null;
    if (profile == null) return;
    profileRepository.updateProfileClickCount(profile.getId());
    domainEventPublisher.publish(new UserProfileClickEvent(profileUser, user));
  }

  public List<Long> 캐시된_누적_설문_기반_추천_프로필_조회(User user) {
    return 캐시된_추천_프로필_조회(
        RedisKey.누적_설문_기반_추천_레디스_키,
        redisUtils::캐시된_누적_설문_기반_추천_프로필_조회,
        user
    );
  }

  public List<Long> 캐시된_일일_설문_기반_추천_프로필_조회(User user) {
    return 캐시된_추천_프로필_조회(
        RedisKey.일일_설문_기반_추천_레디스_키,
        redisUtils::캐시된_일일_설문_기반_추천_프로필_조회,
        user
    );
  }

  @Transactional
  List<Long> 활성_유저_조회(User user) {
    List<Long> 활성_유저_ids = 캐시된_활성_유저_ids_조회();
    if (활성_유저_ids.isEmpty()) {
      활성_유저_캐시_리프레시();
      활성_유저_ids = 캐시된_활성_유저_ids_조회();
    }
    log.info("step=누적설문_추천_활성유저_조회, activeUserCount={}", 활성_유저_ids);

    List<Long> 제외된_유저_ids = 이성_추천에_제외되는_유저_조회(user);
    log.info("step=누적설문_추천_제외유저_조회, excludedUserCount={}", 제외된_유저_ids.size());

    활성_유저_ids.removeAll(제외된_유저_ids);
    log.info("step=누적설문_추천_풀_확정, candidateCount={}", 활성_유저_ids.size());

    return 활성_유저_ids;
  }

  @Transactional
  List<GetUserProfileResponseDto> 유저_일일_추천_기반_프로필_추천(User user) {
    List<AnsweredSurvey> 금일_설문 = 금일_응답한_설문_조회(user);
    if (금일_설문.isEmpty()) {
      log.info("step=금일_설문_응답_부재, todayAnswersSize=0");
      return null;
    }

    Collections.shuffle(금일_설문);

    List<Long> 활성_유저 = 활성_유저_조회(user);

    return 비슷한_응답한_유저_프로필_조회(user, 금일_설문, 활성_유저);
  }

  private List<GetUserProfileResponseDto> 비슷한_응답한_유저_프로필_조회(
      User user,
      List<AnsweredSurvey> todayAnswers,
      List<Long> 활성_유저_ids
  ) {
    Set<GetUserProfileResponseDto> result = new HashSet<>();
    for (AnsweredSurvey todayAnswer : todayAnswers) {
      if (result.size() == 일일_설문_기반_추천_크기) break;

      User 추천_이성 = 유저와_비슷하게_응답한_설문을_랜덤으로_조회(todayAnswer, 활성_유저_ids);
      if (추천_이성 == null) continue;

      result.add(추천_프로필_생성(user, 추천_이성));
    }
    return new ArrayList<>(result);
  }

  private User 유저와_비슷하게_응답한_설문을_랜덤으로_조회(AnsweredSurvey 금일_응답, List<Long> 활성_유저_ids) {
    List<AnsweredSurvey> 비슷한_응답_리스트 = 비슷한_응답_객체_조회(금일_응답, 활성_유저_ids);

    if (비슷한_응답_리스트.isEmpty()) return null;

    int index = ThreadLocalRandom.current().nextInt(비슷한_응답_리스트.size());
    User 추천_이성 = 비슷한_응답_리스트.get(index).getUser();

    비슷한_응답_리스트_로그_남기기(
        금일_응답,
        비슷한_응답_리스트.get(index),
        금일_응답.getUser(),
        추천_이성
    );

    return 추천_이성;
  }

  private List<GetUserProfileResponseDto> 다수_추천_프로필_생성(User requestUser, List<Long> selectedIds) {
    List<User> users                    = userQueryUseCase.findAllByIdIn(selectedIds);
    Map<Long, List<String>> hashtagsMap = 유저_해시태그_맵_조회(users);
    Set<Long> scrappedUserIds           = 유저가_스크랩한_유저_조회(requestUser);
    Map<Long, Float> surveyScoreMap     = surveyScoreCalculator.설문점수_배치_계산(requestUser.getId(), selectedIds);

    return users.stream()
        .filter(u -> surveyScoreMap.containsKey(u.getId()))
        .map(recommended -> {
          Profile profile = recommended.getProfile();
          if (profile == null) return null;
          float 설문점수           = surveyScoreMap.get(recommended.getId());
          int 얼굴인증_여부         = FaceVerify.PASS == profile.getFaceVerify() ? 프로필_얼굴인증_활성화 : 프로필_얼굴인증_비활성화;
          List<String> 해시태그    = hashtagsMap.getOrDefault(recommended.getId(), List.of());
          boolean 스크랩_여부       = scrappedUserIds.contains(recommended.getId());

          GetUserProfileResponseDto dto = GetUserProfileResponseDto.of(
              recommended,
              설문점수,
              해시태그,
              얼굴인증_여부,
              프로필_숨김처리_비활성화,
              requestUser.getMbti(),
              스크랩_여부
          );
          log.info("step=추천이성_프로필_빌드, recommendedUserId={}, age={}, gender={}, nickname={}, matchingScore={}, hashtag={}, faceVerified={}, daysFromLastAccess={}, mbti={}",
              dto.getUserId(), dto.getAge(), dto.getGender(), dto.getNickname(),
              dto.getMatchingScore(), dto.getHashtags(), dto.getVerify(),
              dto.getDaysFromLastAccess(), dto.getMbti());
          return dto;
        })
        .filter(Objects::nonNull)
        .toList();
  }

  private Set<Long> 유저가_스크랩한_유저_조회(User user){
    return scrappedUserRepository.findAllByUser(user).stream()
        .map(s -> s.getScrappedUser().getId())
        .collect(Collectors.toSet());
  }

  private List<Long> 최종_매칭_점수_계산후_이성_추천(Map<Long, Float> scoreMap) {
    Map<Long, Double> 유저_id_매칭_점수_맵 = recommendationDomainService.최종_매칭_점수_계산(scoreMap);
    return 유저_id_매칭_점수_맵
        .entrySet()
        .stream()
        .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
        .map(Map.Entry::getKey)
        .limit(누적_설문_기반_추천_크기)
        .toList();
  }

  private Map<Long, Float> 일정_설문_점수_이상의_추천_후보_조회(Long userId, List<Long> activeUserIds) {
    return surveyScoreCalculator.설문점수_배치_계산(userId, activeUserIds)
        .entrySet()
        .stream()
        .filter(entry -> entry.getValue() >= 설문_매칭_점수_임계값)
        .sorted((entry1, entry2) -> Float.compare(entry2.getValue(), entry1.getValue()))
        .limit(100)
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }

  private List<Long> 캐시된_추천_프로필_조회(
      String keyPrefix,
      Function<String, List<Long>> function,
      User user
  ) {
    Long userId = user.getId();
    if (redisTemplate.hasKey(keyPrefix + userId)) {
      List<Long> cached = function.apply(userId.toString());
      return cached;
    }
    return new ArrayList<>();
  }

  private void 프로필_숨김(
      String redisKeyPrefix,
      User user,
      Long targetUserId,
      Function<User, List<Long>> function
  ) {
    List<Long> profiles = function.apply(user);
    profiles.remove(targetUserId);
    redisUtils.cacheUntilMidnight(
        redisKeyPrefix + user.getId(),
        new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), profiles)
    );
  }

  @SuppressWarnings("unchecked")
  private List<Long> 캐시된_활성_유저_ids_조회() {
    Object cache = redisTemplate.opsForValue().get(RedisKey.활성_유저_레디스_키);
    if (cache instanceof ApiListResponseDto<?> dto && dto.getList() != null) {
      return new ArrayList<>((List<Long>) dto.getList());
    }
    return new ArrayList<>();
  }

  private GetUserProfileResponseDto 추천_프로필_생성(User requestUser, User recommendedUser) {
    Profile profile       = recommendedUser.getProfile();
    float surveyScore     = surveyScoreCalculator.설문_점수_계산(requestUser.getId(), recommendedUser.getId());
    int isVerify          = FaceVerify.PASS == profile.getFaceVerify() ? 프로필_얼굴인증_활성화 : 프로필_얼굴인증_비활성화;
    List<String> hashtags = 유저_해시태그_추출(recommendedUser);
    boolean isScrap       = scrappedUserRepository.existsByUserAndScrappedUser(requestUser, recommendedUser);

    GetUserProfileResponseDto dto = GetUserProfileResponseDto.of(
        recommendedUser,
        surveyScore,
        hashtags,
        isVerify,
        프로필_숨김처리_비활성화,
        requestUser.getMbti(),
        isScrap
    );

    log.info("step=추천이성_프로필_빌드, recommendedUserId={}, age={}, gender={}, nickname={}, matchingScore={}, hashtag={}, faceVerified={}, daysFromLastAccess={}, mbti={}",
        dto.getUserId(), dto.getAge(), dto.getGender(), dto.getNickname(),
        dto.getMatchingScore(), dto.getHashtags(), dto.getVerify(),
        dto.getDaysFromLastAccess(), dto.getMbti());

    return dto;
  }

  private Map<Long, List<String>> 유저_해시태그_맵_조회(List<User> users) {
    return hashtagRepository.findAllByUserIn(users).stream()
        .collect(Collectors.groupingBy(
            h -> h.getUser().getId(),
            Collectors.mapping(Hashtag::getHashtag, Collectors.toList())
        ));
  }

  private List<String> 유저_해시태그_추출(User user) {
    return hashtagRepository.findAllByUser(user)
        .stream()
        .map(Hashtag::getHashtag)
        .toList();
  }

  private List<AnsweredSurvey> 비슷한_응답_객체_조회(AnsweredSurvey todayAnswer, List<Long> 활성_유저_ids) {
    int 설문_번호 = todayAnswer.getSurveyNum();
    int 설문_응답 = todayAnswer.getAnswer();
    return answeredSurveyRepository.findAllByUserIdInAndSurveyNumAndAnswerIn(
        활성_유저_ids,
        설문_번호,
        List.of(설문_응답, 설문_응답 + 1, 설문_응답 - 1)
    );
  }

  private List<AnsweredSurvey> 금일_응답한_설문_조회(User user) {
    return answeredSurveyRepository.findAllByUserAndUpdatedAtAfter(
        user, LocalDate.now().atStartOfDay()
    );
  }

  private List<Long> 회원가입_미완료_유저_ids() {
    return userQueryUseCase.findAllByStatusNot(SignupStatus.COMPLETED)
        .stream()
        .map(User::getId)
        .toList();
  }

  private List<Long> 휴면_계정_ids() {
    return dormantAccountRepository.findAllDormantUserIds();
  }

  private List<Long> 블락된_유저_ids() {
    return blockedUserRepository.findAllBlockedUserIds();
  }

  private List<Long> 정지된_유저_ids() {
    return redisTemplate.keys("suspension::*")
        .stream()
        .map(s -> s.replace("suspension::", ""))
        .map(Long::parseLong)
        .toList();
  }

  private List<Long> 내가_요청_보낸_유저_ids(User user) {
    return matchQueryUseCase.findRequestedUserIdsByRequestUser(user);
  }

  private List<Long> 나에게_요청_보낸_유저_ids(User user) {
    return matchQueryUseCase.findRequestUserIdsByRequestedUser(user);
  }

  private void 비슷한_응답_리스트_로그_남기기(
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
        user.getId(), recommended != null ? recommended.getId() : null,
        todayAnswer.getAnswer(), selectedAnswer.getAnswer());
  }

}
