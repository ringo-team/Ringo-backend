package com.lingo.lingoproject.api.match;

import com.lingo.lingoproject.api.chat.ChatService;
import com.lingo.lingoproject.api.chat.dto.CreateChatroomRequestDto;
import com.lingo.lingoproject.db.domain.AnsweredSurvey;
import com.lingo.lingoproject.db.domain.BlockedUser;
import com.lingo.lingoproject.db.domain.DormantAccount;
import com.lingo.lingoproject.db.domain.Hashtag;
import com.lingo.lingoproject.db.domain.Matching;
import com.lingo.lingoproject.db.domain.Profile;
import com.lingo.lingoproject.db.domain.Recommendation;
import com.lingo.lingoproject.db.domain.ScrappedUser;
import com.lingo.lingoproject.db.domain.Survey;
import com.lingo.lingoproject.db.domain.User;
import com.lingo.lingoproject.db.domain.UserAccessLog;
import com.lingo.lingoproject.db.domain.UserActivityLog;
import com.lingo.lingoproject.db.domain.UserMatchingLog;
import com.lingo.lingoproject.db.domain.enums.ChatType;
import com.lingo.lingoproject.db.domain.enums.MatchingStatus;
import com.lingo.lingoproject.db.domain.enums.SignupStatus;
import com.lingo.lingoproject.common.exception.ErrorCode;
import com.lingo.lingoproject.common.exception.RingoException;
import com.lingo.lingoproject.api.match.dto.GetScrappedUserResponseDto;
import com.lingo.lingoproject.api.match.dto.RelatedSurveyAnswerPairInterface;
import com.lingo.lingoproject.common.notification.FcmService;
import com.lingo.lingoproject.api.match.dto.GetUserProfileResponseDto;
import com.lingo.lingoproject.api.match.dto.MatchScoreResultInterface;
import com.lingo.lingoproject.api.match.dto.MatchingRequestDto;
import com.lingo.lingoproject.api.match.dto.SaveMatchingRequestMessageRequestDto;
import com.lingo.lingoproject.db.repository.AnsweredSurveyRepository;
import com.lingo.lingoproject.db.repository.BlockedFriendRepository;
import com.lingo.lingoproject.db.repository.BlockedUserRepository;
import com.lingo.lingoproject.db.repository.DormantAccountRepository;
import com.lingo.lingoproject.db.repository.HashtagRepository;
import com.lingo.lingoproject.db.repository.MatchingRepository;
import com.lingo.lingoproject.db.repository.ProfileRepository;
import com.lingo.lingoproject.db.repository.RecommendationRepository;
import com.lingo.lingoproject.db.repository.ScrappedUserRepository;
import com.lingo.lingoproject.db.repository.SurveyRepository;
import com.lingo.lingoproject.db.repository.UserAccessLogRepository;
import com.lingo.lingoproject.db.repository.UserActivityLogRepository;
import com.lingo.lingoproject.db.repository.UserMatchingLogRepository;
import com.lingo.lingoproject.db.repository.UserRepository;
import com.lingo.lingoproject.common.utils.ApiListResponseDto;
import com.lingo.lingoproject.common.utils.RedisUtils;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

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
  private final ChatService chatService;
  private final HashtagRepository hashtagRepository;
  private final BlockedUserRepository blockedUserRepository;
  private final RedisUtils redisUtils;
  private final FcmService fcmService;
  private final UserMatchingLogRepository userMatchingLogRepository;
  private final UserAccessLogRepository userAccessLogRepository;
  private final RedisTemplate<String, Object> redisTemplate;
  private final SurveyRepository surveyRepository;
  private final RecommendationRepository recommendationRepository;
  private final ScrappedUserRepository scrappedUserRepository;

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
  private final UserActivityLogRepository userActivityLogRepository;

  @Value("${ringo.config.survey.space_weight}")
  private float SURVEY_SPACE_WEIGHT;
  @Value("${ringo.config.survey.self_representation_weight}")
  private float SURVEY_SELF_REPRESENTATION_WEIGHT;
  @Value("${ringo.config.survey.content_weight}")
  private float SURVEY_CONTENT_WEIGHT;
  @Value("${ringo.config.survey.sharing_weight}")
  private float SURVEY_SHARING_WEIGHT;

  public Matching matchRequest(MatchingRequestDto dto){

    // 유저 검증
    User requestUser = userRepository.findById(dto.requestId())
        .orElseThrow(() -> new RingoException(
            "매칭을 요청한 유저를 찾을 수 없습니다.", ErrorCode.NOT_FOUND_USER, HttpStatus.BAD_REQUEST));
    User requestedUser = userRepository.findById(dto.requestedId())
        .orElseThrow(() -> new RingoException(
            "매칭을 요청 받은 유저를 찾을 수 없습니다.", ErrorCode.NOT_FOUND_USER, HttpStatus.BAD_REQUEST));

    // 매칭 점수 계산
    float matchingScore = calculateMatchScore(requestedUser.getId(), requestUser.getId());
    log.info("""

        requestUserId={},
        requestedUserId={},
        matchingScore={},
        step=매칭요청,
        status=SUCCESS

        """,
        requestUser.getId(),
        requestedUser.getId(),
        matchingScore);

    // 매칭 내용 저장
    Matching matching = Matching.builder()
        .requestedUser(requestedUser)
        .requestUser(requestUser)
        .matchingStatus(MatchingStatus.PRE_REQUESTED)
        .matchingScore(matchingScore)
        .build();

    log.info("""
        ######### 매칭 요청 ###########
        request-user: {},
        requested-user: {},
        matching-status: {},
        matching-score: {},
        matching-date: {}
        #############################
        """,
        requestUser.getId(),
        requestedUser.getId(),
        matching.getMatchingStatus(),
        matching.getMatchingScore(),
        LocalDateTime.now()
        );

    //fcmService.sendFcmNotification(requestedUser, "누군가 매칭 요청을 했어요!", null, NotificationType.MATCHING_REQUEST);

    return matchingRepository.save(matching);
  }

  /**
   * 연결 수락시 채팅방 생성 및 status를 ACCEPTED로 변경
   * 거절시 status만 REJECTED로 변경
   */
  public void responseToRequest(String decision, Long matchingId, User user) {

    Matching matching = matchingRepository.findById(matchingId)
        .orElseThrow(() -> new RingoException("적절하지 않은 매칭 id 입니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));

    Long requestedUserId = matching.getRequestedUser().getId();
    if (!requestedUserId.equals(user.getId())) {
      log.error("""
          
          message=해당 메칭에 응답할 권한이 없습니다.
          authUserId={},
          userId={}, 
          step=잘못된_유저_요청,
          status=FAILED
          
          """, user.getId(), requestedUserId);
      throw new RingoException("매칭 수락 여부를 결정할 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
    }

    switch (decision) {
      case "ACCEPTED" -> handleAcceptedMatching(matching);
      case "REJECTED" -> matching.setMatchingStatus(MatchingStatus.REJECTED);
      default -> throw new RingoException("decision 값이 적절하지 않습니다.", ErrorCode.BAD_PARAMETER, HttpStatus.BAD_REQUEST);
    }

    UserMatchingLog logEntity = UserMatchingLog.builder()
        .userId(user.getId())
        .matchingId(matchingId)
        .status(matching.getMatchingStatus())
        .gender(user.getGender())
        .build();

    log.info("""
        ######### 매칭 요청 ###########
        request-user: {},
        requested-user: {},
        matching-status: {},
        matching-score: {},
        matching-date: {}
        #############################
        """,
        user.getId(),
        requestedUserId,
        matching.getMatchingStatus(),
        matching.getMatchingScore(),
        LocalDateTime.now()
    );

    userMatchingLogRepository.save(logEntity);
    matchingRepository.save(matching);
  }

  private void handleAcceptedMatching(Matching matching){

    // 매칭 수락으로 변경
    matching.setMatchingStatus(MatchingStatus.ACCEPTED);

    matchingRepository.save(matching);

    // 채팅방 생성
    chatService.createChatroom(
        new CreateChatroomRequestDto(
            matching.getRequestUser().getId(),
            matching.getRequestedUser().getId(),
            ChatType.USER.toString()
        )
    );

    // fcm 요청 전송
    // fcmService.sendFcmNotification(matching.getRequestUser(), "누군가 요청을 수락했어요", null, NotificationType.MATCHING_ACCEPTED);
  }

  @Transactional
  public void cacheAllActiveUserIds(){
    LocalDateTime startDay = LocalDate.now().minusDays(ACTIVE_DAY_DURATION).atStartOfDay();
    List<Long> activeUserIds = userActivityLogRepository.findAllByStartAfter(startDay)
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


  @Transactional
  public List<GetUserProfileResponseDto> recommendByCumulativeSurvey(User user){

    Long userId = user.getId();

    // 캐시 조회
    if(redisTemplate.hasKey(CUMULATIVE_RECOMMENDATION_REDIS_KEY_PREFIX + userId)){
      List<GetUserProfileResponseDto> responses = redisUtils.getRecommendedUserForCumulativeSurvey(userId.toString());
      if (responses != null && responses.size() == MAX_CUMULATIVE_SURVEY_BASED_RECOMMENDATION_SIZE) return responses;
    }

    List<Long> activeUserIds = getActiveUserIds();

    // 레디스에 존재하는 active 유저(14일 안에 접속한 유저)를 가져온다.
    if (activeUserIds.isEmpty()){
      cacheAllActiveUserIds();
      activeUserIds = getActiveUserIds();
    }

    log.info("""
        active-user-size: {}
        """, activeUserIds.size());

    // 이성추천에서 배제해야할 유저를 가져온다.
    List<Long> excludedUserIds = getExcludedUserIdsForRecommendation(user);

    log.info("""
        exclueded-user-size: {}
        """, excludedUserIds.size());

    activeUserIds.removeAll(excludedUserIds);

    log.info("""
        추천 이성 풀 사이즈: {}
        """, activeUserIds.size());

    List<Long> userIdsWithHighMatchScore = userRepository.findAllByIdIn(activeUserIds)
        .stream()
        .filter(u -> u.getStatus() == SignupStatus.COMPLETED)
        .map(User::getId)
        .map(id -> {
          float matchingScore = calculateMatchScore(userId, id);
          return new UserMatchingScoreMapping(id, matchingScore);
        })
        .filter(m -> m.matchingScore >= MATCHING_SCORE_THRESHOLD)
        .map(UserMatchingScoreMapping::getId)
        .collect(Collectors.toList());

    // -------------------- 추천이성 섞고 앞에 n 개 뽑기 ----------------------- //

    int n = Math.min(userIdsWithHighMatchScore.size(),
        MAX_CUMULATIVE_SURVEY_BASED_RECOMMENDATION_SIZE);
    ThreadLocalRandom random = ThreadLocalRandom.current();
    for (int i = 0; i < n; i++){
      int j = random.nextInt(i, userIdsWithHighMatchScore.size());
      Collections.swap(userIdsWithHighMatchScore, i, j);
    }
    userIdsWithHighMatchScore = userIdsWithHighMatchScore.subList(0, n);
    log.info("""
        추천이성ids: {}
        """, userIdsWithHighMatchScore);

    //--------------------- 추천된 이성의 프로필을 set에 저장 --------------------- //
    //  makeUserProfileAndAddInCollection 함수가 recommendedUserProfileSet 에 유저 정보를 저장 //
    Set<GetUserProfileResponseDto> recommendedUserProfileSet = new HashSet<>();
    userRepository.findAllByIdIn(userIdsWithHighMatchScore).forEach(recommendedUser ->
        makeUserProfileAndAddInCollection(recommendedUserProfileSet, user, recommendedUser)
    );
    List<GetUserProfileResponseDto> recommendedUserList = new ArrayList<>(recommendedUserProfileSet);

    // 캐시 저장
    redisUtils.cacheUntilMidnight(CUMULATIVE_RECOMMENDATION_REDIS_KEY_PREFIX + userId, new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), recommendedUserList));

    return recommendedUserList;
  }

  private List<Long> getActiveUserIds(){
    Object cache = redisTemplate.opsForValue().get(REDIS_ACTIVE_USER_IDS);
    if (cache instanceof ApiListResponseDto<?> dto){
      return new ArrayList<>((List<Long>) dto.getList());
    }
    return new ArrayList<>();
  }

  static class UserMatchingScoreMapping {
    Long userId;
    Float matchingScore;

    UserMatchingScoreMapping(Long userId, float matchingScore){
      this.userId = userId;
      this.matchingScore = matchingScore;
    }

    public Long getId(){
      return userId;
    }
  }

  public List<GetUserProfileResponseDto> recommendUserByDailySurvey(User user){

    Long userId = user.getId();

    //캐시 조회
    if (redisTemplate.hasKey(DAILY_RECOMMENDATION_REDIS_KEY_PREFIX + userId)){
      List<GetUserProfileResponseDto> responses = redisUtils.getRecommendUserForDailySurvey(userId.toString());
      responses.forEach(response ->
        log.info("""
          
          ######## 캐시조회된 이성 정보 #########
          request-user-id: {}
          user-id: {},
          user-age: {},
          user-gender: {},
          user-nickname: {},
          matching-score: {},
          matching-status: {},
          hashtags: {},
          face-verify: {},
          days-from-last-access: {},
          mbti: {}
          ###############################
          
          """,
            user.getId(),
            response.getUserId(),
            response.getAge(),
            response.getGender(),
            response.getNickname(),
            response.getMatchingScore(),
            response.getMatchingStatus(),
            response.getHashtags(),
            response.getVerify(),
            response.getDaysFromLastAccess(),
            response.getMbti()
        )
      );

      if (responses.size() == MAX_RECOMMENDATION_SIZE_FOR_DAILY_SURVEY) return responses;
    }

    // 오늘 응답한 설문의 응답값을 조회
    List<AnsweredSurvey> todayAnsweredSurveys = answeredSurveyRepository.findAllByUserAndUpdatedAtAfter(
        user,
        LocalDate.now().atStartOfDay()
    );

    // 만약 설문을 완료하지 않았다면 추천받을 수 없음.
    if (todayAnsweredSurveys.isEmpty()){
      return null;
    }

    Collections.shuffle(todayAnsweredSurveys);

    // 추천되지 않아야할 유저를 조회
    List<Long> excludedUserIds = getExcludedUserIdsForRecommendation(user);

    log.info("""
        exclueded-user-size: {}
        """, excludedUserIds.size());

    // 유저와 유사한 응답을 한 이성을 추천해줌
    List<GetUserProfileResponseDto> recommendUserProfileList = new ArrayList<>();
    for (AnsweredSurvey todayAnsweredSurvey : todayAnsweredSurveys) {

      // 4명만 추천해줌
      if (recommendUserProfileList.size() == MAX_RECOMMENDATION_SIZE_FOR_DAILY_SURVEY) break;

      int answer = todayAnsweredSurvey.getSurveyNum();
      // 유사한 (+/- 1) 응답을 한 유저들을 조회
      List<AnsweredSurvey> matchingAnsweredSurveyList = answeredSurveyRepository.findAllByUserIdNotInAndAnswerAndSurveyNumIn(
          excludedUserIds,
          todayAnsweredSurvey.getAnswer(),
          List.of(answer, answer + 1, answer - 1)
      );

      // 유사한 응답을 한 유저가 없으면 continue
      if (matchingAnsweredSurveyList.isEmpty()){
        log.info("매칭되는 설문이 존재하지 않습니다. matched survey list size= {}" , 0);
        continue;
      }

      // 유사한 응답을 한 유저들 중 한 사람을 뽑음
      ThreadLocalRandom random = ThreadLocalRandom.current();
      int n = random.nextInt(matchingAnsweredSurveyList.size());
      User recommendedUser = matchingAnsweredSurveyList.get(n).getUser();

      log.info("""
          
          유사한 설문을 한 설문의 개수: {},
          선택된 설문 id: {} | 선택된 설문 번호: {}
          유저 id: {} | 선택된 유저 id: {}
          유저의 설문 응답: {} | 선택된 유저의 읍답: {}
          
          """,
          matchingAnsweredSurveyList.size(),
          matchingAnsweredSurveyList.get(n).getId(),
          matchingAnsweredSurveyList.get(n).getSurveyNum(),
          user.getId(),
          recommendedUser.getId(),
          answer,
          matchingAnsweredSurveyList.get(n).getAnswer()
          );

      // recommendUserProfileList에 유저 프로필 정보를 저장함
      makeUserProfileAndAddInCollection(recommendUserProfileList, user, recommendedUser);
    }

    // 캐시 저장
    redisUtils.cacheUntilMidnight(DAILY_RECOMMENDATION_REDIS_KEY_PREFIX + userId, new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), recommendUserProfileList));
    return recommendUserProfileList;
  }

  public void hideRecommendedUser(User user, Long recommendedUserId){

    Long userId = user.getId();

    getCacheDataAndSetHideFlag(CUMULATIVE_RECOMMENDATION_REDIS_KEY_PREFIX, userId, recommendedUserId);
    getCacheDataAndSetHideFlag(DAILY_RECOMMENDATION_REDIS_KEY_PREFIX, userId, recommendedUserId);

  }

  private void getCacheDataAndSetHideFlag(String redisKeyPrefix, Long userId, Long recommendedUserId){
    if(redisTemplate.hasKey(redisKeyPrefix + userId)){
      List<GetUserProfileResponseDto> responses = redisUtils.getRecommendedUserForCumulativeSurvey(userId.toString());
      if (responses == null) return;
      setHideFlagOnUserProfile(responses, recommendedUserId, userId);
      redisUtils.cacheUntilMidnight(redisKeyPrefix + userId, new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), responses));
    }
  }

  private void setHideFlagOnUserProfile(List<GetUserProfileResponseDto> responses, Long recommendedUserId, Long userId){
    for (GetUserProfileResponseDto response : responses){
      if (response.getUserId().equals(recommendedUserId)){
        response.setHide(HIDE_PROFILE_FLAG);
        log.info("""
            hide-flag: 0 -----> {}
            user-id: {} 가 추천_이성_id: {} 를 가렸습니다.
            """,
            response.getHide(),
            userId,
            recommendedUserId
        );
        return;
      }
    }
  }

  public void makeUserProfileAndAddInCollection(Collection<GetUserProfileResponseDto> collection, User user, User recommendedUser){

    Profile profile = recommendedUser.getProfile();

    List<String> hashtags = hashtagRepository.findAllByUser(recommendedUser)
        .stream()
        .map(Hashtag::getHashtag)
        .toList();

    UserAccessLog access = userAccessLogRepository.findFirstByUserIdOrderByCreateAtDesc(recommendedUser.getId());

    GetUserProfileResponseDto response = GetUserProfileResponseDto.builder()
        .userId(recommendedUser.getId())
        .age(LocalDate.now().getYear() - recommendedUser.getBirthday().getYear())
        .gender(recommendedUser.getGender().toString())
        .nickname(recommendedUser.getNickname())
        .profileUrl(profile.getImageUrl())
        .matchingScore(calculateMatchScore(user.getId(), recommendedUser.getId()))
        .hashtags(hashtags)
        .hide(EXPOSE_PROFILE_FLAG)
        .verify(profile.isVerified() ? PROFILE_VERIFICATION_FLAG : PROFILE_NON_VERIFICATION_FLAG)
        .daysFromLastAccess((int) ChronoUnit.DAYS.between(access.getCreateAt(), LocalDateTime.now()))
        .mbti(user.getMbti())
        .build();

    log.info("""
          
          ######## 추천 이성 정보 #########
          user-id: {},
          user-age: {},
          user-gender: {},
          user-nickname: {},
          matching-score: {},
          matching-status: {},
          hashtags: {},
          face-verify: {},
          days-from-last-access: {},
          mbti: {}
          ###############################
          
          """,
        response.getUserId(),
        response.getAge(),
        response.getGender(),
        response.getNickname(),
        response.getMatchingScore(),
        response.getMatchingStatus(),
        response.getHashtags(),
        response.getVerify(),
        response.getDaysFromLastAccess(),
        response.getMbti()
    );

    collection.add(response);
  }
  
  public List<Long> getExcludedUserIdsForRecommendation(User user){

    Long userId = user.getId();

    // 연락처에 존재하는 유저
    List<Long> blockedFriendUserIds = blockedFriendRepository.findUsersMutuallyBlockedWith(userId);

    // 휴면 계정인 유저
    List<Long> dormantUserIds = dormantAccountRepository.findAll()
        .stream()
        .map(DormantAccount::getUser)
        .map(User::getId)
        .toList();

    // block되거나 정지된 유저
    List<Long> blockedUserIds = blockedUserRepository.findAll()
        .stream()
        .map(BlockedUser::getBlockedUserId)
        .toList();

    // 계정 중지된 유저
    List<Long> suspendedUserIds = redisTemplate.keys("suspension::*")
        .stream()
        .map(s -> s.replace("suspension::", ""))
        .map(Long::parseLong)
        .toList();

    List<Long> requestUserToUser = matchingRepository.findAllByRequestedUser(user)
        .stream()
        .map(Matching::getRequestUser)
        .map(User::getId)
        .toList();

    List<Long> requestedUserFromUser = matchingRepository.findAllByRequestUser(user)
        .stream()
        .map(Matching::getRequestedUser)
        .map(User::getId)
        .toList();


    Set<Long> excludedUserId = new HashSet<>();
    excludedUserId.add(userId);
    excludedUserId.addAll(blockedFriendUserIds);
    excludedUserId.addAll(dormantUserIds);
    excludedUserId.addAll(blockedUserIds);
    excludedUserId.addAll(suspendedUserIds);
    excludedUserId.addAll(requestUserToUser);
    excludedUserId.addAll(requestedUserFromUser);

    log.info("""
        제외된 유저들의 id: {}
        """, excludedUserId);

    return new ArrayList<>(excludedUserId);
  }


  public float calculateMatchScore(Long user1Id, Long user2Id){
    List<MatchScoreResultInterface> list = answeredSurveyRepository.calcMatchScore(user1Id, user2Id);
    float score = 0;
    for(MatchScoreResultInterface result : list){
      switch(result.getCategory()){
        case "SPACE":
          score += result.getAvgAnswer() * SURVEY_SPACE_WEIGHT;
          break;
        case "SELF_REPRESENTATION":
          score += result.getAvgAnswer() * SURVEY_SELF_REPRESENTATION_WEIGHT;
          break;
        case "SHARING":
          score += result.getAvgAnswer() * SURVEY_SHARING_WEIGHT;
          break;
        case "CONTENT":
          score += result.getAvgAnswer() * SURVEY_CONTENT_WEIGHT;
          break;
      }
    }
    return score;
  }

  /**
   * 내가 매칭 요청한 사람들의 정보
   */
  public List<GetUserProfileResponseDto> getUserIdWhoRequestedByMe(User requestUser){
    // 매칭 조회
    List<Matching> matchings = matchingRepository.findAllByRequestUser(requestUser);

    // 매칭 id 조회
    List<Long> matchingIds = matchings.stream()
        .map(Matching::getId)
        .toList();

    // 매칭 요청 받은 유저들의 정보 조회
    List<GetUserProfileResponseDto> requestedUserProfileDtoList =
        profileRepository.getRequestedUserProfilesByMatchingIds(matchingIds);

    addHashtagInEachProfileDto(requestedUserProfileDtoList);

    return requestedUserProfileDtoList;
  }

  public List<GetUserProfileResponseDto> getUserIdWhoRequestToMe(User requestedUser){
    List<Matching> matchings = matchingRepository.findAllByRequestedUser(requestedUser);

    List<Long> matchingIds = matchings.stream()
        .filter(m -> m.getMatchingStatus() != MatchingStatus.PRE_REQUESTED)
        .map(Matching::getId)
        .toList();

    List<GetUserProfileResponseDto> requestUserProfileDtoList =
        profileRepository.getRequestUserProfilesByMatchingIds(matchingIds);

    addHashtagInEachProfileDto(requestUserProfileDtoList);

    return requestUserProfileDtoList;
  }

  private void addHashtagInEachProfileDto(List<GetUserProfileResponseDto> profiles){
    profiles.forEach(profile -> {
      User user = userRepository.findById(profile.getUserId()).orElse(null);
      if (user == null) return;
      List<String> hashtags = hashtagRepository.findAllByUser(user)
          .stream()
          .map(Hashtag::getHashtag)
          .toList();
      profile.setHashtags(hashtags);
      profile.setAge(LocalDate.now().getYear() - user.getBirthday().getYear());
      log.info("""
          
          ######## 매칭 보낸/받은 이성 정보 #########
          user-id: {},
          user-age: {},
          user-gender: {},
          user-nickname: {},
          matching-score: {},
          matching-status: {},
          hashtags: {},
          face-verify: {},
          days-from-last-access: {},
          mbti: {}
          ###############################
          
          """,
          profile.getUserId(),
          profile.getAge(),
          profile.getGender(),
          profile.getNickname(),
          profile.getMatchingScore(),
          profile.getMatchingStatus(),
          profile.getHashtags(),
          profile.getVerify(),
          profile.getDaysFromLastAccess(),
          profile.getMbti()
          );
    });
  }

  @Transactional
  public void deleteMatching(Long matchingId, User user){

    Matching match = matchingRepository.findById(matchingId)
        .orElseThrow(() -> new RingoException("해당 매칭을 찾을 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));

    Long requestedUserId = match.getRequestedUser().getId();
    Long requestUserId = match.getRequestUser().getId();

    log.info("""
        
        매칭 id: {},
        요청자 id: {} 가 매칭 철회를 요청하였습니다.
        
        """, matchingId, user.getId());

    if (!(requestedUserId.equals(user.getId()) || requestUserId.equals(user.getId()))){
      log.error("""
          
          message=해당 매칭을 철회할 권한이 없습니다.
          user-id: {},
          requested-user-id: {}
          step=잘못된_유저_요청,
          status=FAILED"
          
          """, user.getId(), requestedUserId);
      throw new RingoException("매칭을 삭제할 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.BAD_REQUEST);
    }

    matchingRepository.deleteById(matchingId);
  }

  @Transactional
  public void saveMatchingRequestMessage(SaveMatchingRequestMessageRequestDto dto, Long matchingId, User user){

    Matching matching = matchingRepository.findById(matchingId)
        .orElseThrow(() -> new RingoException("해당 매칭을 찾을 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));

    User requestUser = matching.getRequestUser();
    if (!requestUser.getId().equals(user.getId())){
      log.error("""
          
          authUserId={}, 
          userId={}, 
          step=잘못된_유저_요청, 
          status=FAILED
          
          """, user.getId(), requestUser.getId());
      throw new RingoException("요청 메세지를 저장 및 수정할 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.BAD_REQUEST);
    }
    matching.setMatchingStatus(MatchingStatus.PENDING);
    matching.setMatchingRequestMessage(dto.message());

    // 로그에 저장
    UserMatchingLog logEntity = UserMatchingLog.builder()
        .userId(requestUser.getId())
        .matchingId(matching.getId())
        .status(MatchingStatus.PENDING)
        .gender(requestUser.getGender())
        .build();

    log.info("""
        
        matching-id: {}
        request-user-id: {}, 가 보낸 메세지를 저장하였습니다.
        matching-status: {}.
        request-user-gender: {}
        
        """,
        matching.getId(),
        requestUser.getId(),
        matching.getMatchingStatus(),
        requestUser.getGender()
        );

    matchingRepository.save(matching);
    userMatchingLogRepository.save(logEntity);

    // 유저 알림
    //fcmService.sendFcmNotification(matching.getRequestedUser(), "누군가 매칭을 요청했어요", null, NotificationType.MATCHING_REQUEST);
  }

  public String getMatchingRequestMessage(Long matchingId, User user){

    Matching match = matchingRepository.findById(matchingId)
        .orElseThrow(() -> new RingoException("해당 매칭을 찾을 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));

    Long requestedUserId = match.getRequestedUser().getId();
    Long requestUserId = match.getRequestUser().getId();
    if (!(requestedUserId.equals(user.getId()) || requestUserId.equals(user.getId()))){
      log.error("""
          
          authUserId={}, 
          step=잘못된_유저_요청, 
          status=FAILED
          
          """, user.getId());
      throw new RingoException("해당 매칭의 요청 메세지를 확인할 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.BAD_REQUEST);
    }

    return match.getMatchingRequestMessage();
  }

  @Transactional
  public List<String> getMatchedReason(Long user1, Long user2){

    List<Integer> higherAnswer = List.of(3, 4, 5);

    return getSortedRelatedAnswerPairs(user1, user2)
        .stream()
        .map( as -> {
          Survey survey = surveyRepository.findById(as.getSurveyId())
              .orElseThrow(() -> new RingoException("적절하지 않은 설문아이디입니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR));
          Integer answer = as.getAnswer();
          if (higherAnswer.contains(answer)){
            return survey.getMatchedReasonForHigherAnswer();
          }
          else{
            return survey.getMatchedReasonForLowerAnswer();
          }
        })
        .limit(5)
        .toList();
  }

  public List<RelatedSurveyAnswerPairInterface> getSortedRelatedAnswerPairs(Long user1, Long user2){
    List<RelatedSurveyAnswerPairInterface> matchedSurveys = answeredSurveyRepository.getRelatedSurveyAnswerPairs(user1, user2);
    return matchedSurveys.stream()
        .filter(s -> Math.abs(s.getAnswer() - s.getConfrontAnswer()) <= 2 )
        .sorted((s1, s2) -> {
          int score1 = getRelatedAnswerPairToScore(s1.getAnswer(), s1.getConfrontAnswer());
          int score2 = getRelatedAnswerPairToScore(s2.getAnswer(), s1.getConfrontAnswer());
          return score2 - score1;
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
  public int getRelatedAnswerPairToScore(int answer, int confrontAnswer){
    int score = 0;

    // 동일한
    if (Math.abs(answer - confrontAnswer) == 0) score += 10;
    else if (Math.abs(answer - confrontAnswer) == 1) score += 7;
    else score += 0;

    if ((answer == 5 || answer == 1) || (confrontAnswer == 5 || confrontAnswer == 1)) score += 5;
    else if ((answer == 4 || answer == 2) || (confrontAnswer == 4 || confrontAnswer == 2)) score += 3;

    return score;
  }

  public List<Recommendation> getRecommendationForMatchedPreference(Long user1, Long user2) {
    List<Integer> higherAnswer = List.of(3, 4, 5);

    return getSortedRelatedAnswerPairs(user1, user2)
        .stream()
        .map(as ->{
          Survey survey = surveyRepository.findById(as.getSurveyId()).orElseThrow(() -> new RingoException("설문을 찾을 수 없습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR));
          Integer answer = as.getAnswer();
          if (higherAnswer.contains(answer)) {
            return getRecommendationListByCategory(survey.getKeywordForHigherAnswer());
          }
          return getRecommendationListByCategory(survey.getKeywordForLowerAnswer());
        })
        .flatMap(Collection::stream)
        .limit(1000)
        .toList();
  }

  public List<Recommendation> getRecommendationsForIndividualSurvey(User user){
    List<AnsweredSurvey> answeredSurveyList = answeredSurveyRepository.findAllByUser(user);

    return answeredSurveyList.stream()
        .sorted((s1, s2) -> {
          int score1 = getAnswerToScore(s1.getAnswer());
          int score2 = getAnswerToScore(s2.getAnswer());
          return score2 - score1;
        })
        .map(s -> {
          Survey survey = surveyRepository.findBySurveyNum(s.getSurveyNum());
          if (List.of(3, 4, 5).contains(s.getAnswer())){
            String keyword = survey.getKeywordForHigherAnswer();
            return getRecommendationListByCategory(keyword);
          }
          else{
            String keyword = survey.getKeywordForLowerAnswer();
            return getRecommendationListByCategory(keyword);
          }
        })
        .flatMap(Collection::stream)
        .limit(5)
        .toList();
  }

  /*
   *  1, 5 번처럼 극단적인 값에 더 높은 점수를 부여함
   */
  public int getAnswerToScore(int answer){
    if (answer == 1 || answer == 5) return 1;
    else if (answer == 2 || answer == 4) return 0;
    else return -1;
  }

  public List<Recommendation> getRecommendationListByCategory(String keywords){
    List<Recommendation> list = new ArrayList<>();
    List<String> keywordList = Arrays.stream(keywords.split(",")).toList();
    keywordList.forEach(category -> {
      List<Recommendation> recommendations = recommendationRepository.findAllByKeywordContainingIgnoreCase(category);
      list.addAll(recommendations);
    });
    return list;
  }

  public void scrapUser(Long recommendedUserId, User user){
    Long userId = user.getId();

    List<Long> recommendUserIdListForCumulativeSurvey =
        Optional.ofNullable(redisUtils.getRecommendedUserForCumulativeSurvey(userId.toString()))
            .orElseGet(Collections::emptyList)
            .stream()
            .map(GetUserProfileResponseDto::getUserId)
            .toList();

    log.info("누적 설문을 통한 이성 추천 리스트: {}", recommendUserIdListForCumulativeSurvey);

    List<Long> recommendUserIdListForDailySurvey =
        Optional.ofNullable(redisUtils.getRecommendUserForDailySurvey(userId.toString()))
            .orElseGet(Collections::emptyList)
            .stream()
            .map(GetUserProfileResponseDto::getUserId)
            .toList();

    log.info("일일 설문을 통한 이성 추천 리스트: {}", recommendUserIdListForDailySurvey);

    List<Long> recommededUserIdList = new ArrayList<>();
    recommededUserIdList.addAll(recommendUserIdListForCumulativeSurvey);
    recommededUserIdList.addAll(recommendUserIdListForDailySurvey);

    if (!recommededUserIdList.contains(recommendedUserId)) {
      log.info("""
          
          요청 유저 id: {},
          스크랩 유저 id: {},
          유저의 추천 이성 리스트: {}
          
          """,
          user.getId(),
          recommendedUserId,
          recommededUserIdList
          );
      return;
    }

    User recommendedUser = userRepository.findById(recommendedUserId).orElseThrow(() -> new RingoException("유저를 스크랩하던 도중 유저를 찾을 수 없습니다.", ErrorCode.NOT_FOUND_USER, HttpStatus.BAD_REQUEST));

    scrappedUserRepository.save(
        ScrappedUser.builder()
            .user(user)
            .scrappedUser(recommendedUser)
            .build()
    );
  }

  public List<GetScrappedUserResponseDto> getScrappedUser(User user){
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
              profile.isVerified() ? 1 : 0
          );
        })
        .toList();

  }

}
