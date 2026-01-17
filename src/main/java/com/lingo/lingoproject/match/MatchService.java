package com.lingo.lingoproject.match;

import com.lingo.lingoproject.chat.ChatService;
import com.lingo.lingoproject.chat.dto.CreateChatroomRequestDto;
import com.lingo.lingoproject.domain.AnsweredSurvey;
import com.lingo.lingoproject.domain.BlockedUser;
import com.lingo.lingoproject.domain.DormantAccount;
import com.lingo.lingoproject.domain.Hashtag;
import com.lingo.lingoproject.domain.Matching;
import com.lingo.lingoproject.domain.Profile;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.domain.UserAccessLog;
import com.lingo.lingoproject.domain.UserMatchingLog;
import com.lingo.lingoproject.domain.enums.ChatType;
import com.lingo.lingoproject.domain.enums.MatchingStatus;
import com.lingo.lingoproject.exception.ErrorCode;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.fcm.FcmService;
import com.lingo.lingoproject.match.dto.GetUserProfileResponseDto;
import com.lingo.lingoproject.match.dto.MatchScoreResultInterface;
import com.lingo.lingoproject.match.dto.MatchingRequestDto;
import com.lingo.lingoproject.match.dto.SaveMatchingRequestMessageRequestDto;
import com.lingo.lingoproject.repository.AnsweredSurveyRepository;
import com.lingo.lingoproject.repository.BlockedFriendRepository;
import com.lingo.lingoproject.repository.BlockedUserRepository;
import com.lingo.lingoproject.repository.DormantAccountRepository;
import com.lingo.lingoproject.repository.HashtagRepository;
import com.lingo.lingoproject.repository.MatchingRepository;
import com.lingo.lingoproject.repository.ProfileRepository;
import com.lingo.lingoproject.repository.UserAccessLogRepository;
import com.lingo.lingoproject.repository.UserMatchingLogRepository;
import com.lingo.lingoproject.repository.UserRepository;
import com.lingo.lingoproject.utils.JsonListWrapper;
import com.lingo.lingoproject.utils.RedisUtils;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

  private final int MAX_RECOMMENDATION_SIZE_FOR_CUMULATIVE_SURVEY = 4;
  private final int MAX_RECOMMENDATION_SIZE_FOR_DAILY_SURVEY = 4;
  private final float LIMIT_OF_MATCHING_SCORE = 0.6f;
  private final int ACTIVE_DAY_DURATION = 14;
  private final int HIDE_PROFILE_FLAG = 1;
  private final int EXPOSE_PROFILE_FLAG = 0;
  private final String REDIS_ACTIVE_USER_IDS = "redis:active:ids";

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
    float matchingScore = calcMatchScore(requestedUser.getId(), requestUser.getId());
    log.info("requestUserId={}, requestedUserId={}, matchingScore={}, step=매칭요청, status=SUCCESS", requestUser.getId(), requestedUser.getId(), matchingScore);
    if(!isMatch(matchingScore)){
      Matching matching = Matching.builder()
          .requestedUser(requestedUser)
          .requestUser(requestUser)
          .matchingStatus(MatchingStatus.UNSATISFIED)
          .matchingScore(matchingScore)
          .build();
      matchingRepository.save(matching);
      return null;
    }

    // 매칭 내용 저장
    Matching matching = Matching.builder()
        .requestedUser(requestedUser)
        .requestUser(requestUser)
        .matchingStatus(MatchingStatus.PRE_REQUESTED)
        .matchingScore(matchingScore)
        .build();

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
      log.error("authUserId={}, userId={}, step=잘못된_유저_요청, status=FAILED", user.getId(), requestedUserId);
      throw new RingoException("매칭 수락 여부를 결정할 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
    }

    switch (decision) {
      case "ACCEPTED" -> handleAcceptedMatching(matching);
      case "REJECTED" -> matching.setMatchingStatus(MatchingStatus.REJECTED);
      default -> throw new RingoException("decision 값이 적절하지 않습니다.", ErrorCode.BAD_PARAMETER, HttpStatus.BAD_REQUEST);
    }

    UserMatchingLog log = UserMatchingLog.builder()
        .userId(user.getId())
        .matchingId(matchingId)
        .status(matching.getMatchingStatus())
        .gender(user.getGender())
        .build();

    userMatchingLogRepository.save(log);
    matchingRepository.save(matching);
  }

  private void handleAcceptedMatching(Matching matching){

    // 매칭 수락으로 변경
    matching.setMatchingStatus(MatchingStatus.ACCEPTED);

    // 채팅방 생성
    chatService.createChatroom(
        new CreateChatroomRequestDto(
            matching.getRequestUser().getId(),
            matching.getRequestedUser().getId(),
            ChatType.USER.toString()
        )
    );

    // fcm 요청 전송
    fcmService.sendFcmNotification(matching.getRequestUser(), "누군가 요청을 수락했어요", null);
  }

  @Transactional
  public void syncRedisAllActiveUsers(){
    LocalDateTime startDay = LocalDate.now().minusDays(ACTIVE_DAY_DURATION).atStartOfDay();
    List<Long> activeUserIds = userAccessLogRepository.findAllByCreateAtAfter(startDay)
        .stream().map(UserAccessLog::getUserId).toList();
    redisTemplate.delete(REDIS_ACTIVE_USER_IDS);
    redisTemplate.opsForValue().set(
        REDIS_ACTIVE_USER_IDS,
        new JsonListWrapper<>(ErrorCode.SUCCESS.toString(), activeUserIds),
        1,
        TimeUnit.HOURS
    );
  }


  @Transactional
  public List<GetUserProfileResponseDto> recommendByCumulativeSurvey(User user){

    Long userId = user.getId();

    // 캐시 조회
    if(redisUtils.containsRecommendedUser(userId.toString())){
      List<GetUserProfileResponseDto> responses = redisUtils.getRecommendedUserForCumulativeSurvey(userId.toString());
      if (responses.size() == MAX_RECOMMENDATION_SIZE_FOR_CUMULATIVE_SURVEY) return responses;
    }

    JsonListWrapper<Long> redisListWrapper = (JsonListWrapper<Long>) redisTemplate.opsForValue().get(REDIS_ACTIVE_USER_IDS);
    List<Long> activeUserIds = redisListWrapper != null ? redisListWrapper.getList() : null;

    // 레디스에 존재하는 active 유저(14일 안에 접속한 유저)를 가져온다.
    if (activeUserIds == null || activeUserIds.isEmpty()){
      syncRedisAllActiveUsers();
      activeUserIds = ((JsonListWrapper<Long>) redisTemplate.opsForValue().get(REDIS_ACTIVE_USER_IDS))
          .getList().stream().map(v -> (long) v).collect(Collectors.toList());
    }else{
      activeUserIds = ((JsonListWrapper<Long>) redisTemplate.opsForValue().get(REDIS_ACTIVE_USER_IDS))
          .getList().stream().map(v -> (long) v).collect(Collectors.toList());;
    }

    // 이성추천에서 배제해야할 유저를 가져온다.
    List<Long> excludedUserIds = getExcludedUserIdsForRecommendation(userId);
    activeUserIds.removeAll(excludedUserIds);


    // 임계치 이상의 매칭도를 가진 유저를 랜덤으로 4명 추천해준다.
    List<Long> closeRelationUserIds = activeUserIds
        .stream()
        .map(id -> {
          float matchingScore = calcMatchScore(userId, id);
          return new UserMatchingScoreMapping(id, matchingScore);
        })
        .filter(m -> m.matchingScore >= LIMIT_OF_MATCHING_SCORE)
        .map(UserMatchingScoreMapping::getId)
        .collect(Collectors.toList());

    Collections.shuffle(closeRelationUserIds);
    closeRelationUserIds = closeRelationUserIds.subList(0, MAX_RECOMMENDATION_SIZE_FOR_CUMULATIVE_SURVEY);

    // 추천된 이성의 프로필을 set에 저장한다.
    Set<GetUserProfileResponseDto> recommendedUserProfileSet = new HashSet<>();
    userRepository.findAllByIdIn(closeRelationUserIds)
        .forEach(recommendedUser -> addUserProfileToCollection(recommendedUserProfileSet, user, recommendedUser));

    List<GetUserProfileResponseDto> recommendUserList = new ArrayList<>(recommendedUserProfileSet);

    // 캐시 저장
    redisUtils.saveRecommendedUserForCumulativeSurvey(userId.toString(), recommendUserList);

    return recommendUserList;
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
    if (redisUtils.containsRecommendUserForDailySurvey(userId.toString())){
      List<GetUserProfileResponseDto> responses = redisUtils.getRecommendUserForDailySurvey(userId.toString());
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

    // 유저와 유사한 응답을 한 이성을 추천해줌
    List<GetUserProfileResponseDto> recommendUserProfileList = new ArrayList<>();
    for (AnsweredSurvey todayAnsweredSurvey : todayAnsweredSurveys) {

      // 4명만 추천해줌
      if (recommendUserProfileList.size() == MAX_RECOMMENDATION_SIZE_FOR_DAILY_SURVEY) break;

      // 추천되지 않아야할 유저를 조회
      List<User> excludedUsers = convertIdListToUserList(getExcludedUserIdsForRecommendation(user.getId()));

      int answer = todayAnsweredSurvey.getSurveyNum();
      // 유사한 (+/- 1) 응답을 한 유저들을 조회
      List<AnsweredSurvey> matchingAnsweredSurveyList = answeredSurveyRepository.findAllByUserNotInAndAnswerAndSurveyNumIn(
          excludedUsers,
          todayAnsweredSurvey.getAnswer(),
          List.of(answer, answer + 1, answer - 1)
      );

      // 유사한 응답을 한 유저가 없으면 continue
      if (matchingAnsweredSurveyList.isEmpty()){ continue; }

      // 유사한 응답을 한 유저들 중 한 사람을 뽑음
      Collections.shuffle(matchingAnsweredSurveyList);
      User recommendedUser = matchingAnsweredSurveyList.isEmpty() ?
          null : matchingAnsweredSurveyList.getFirst().getUser();
      if (recommendedUser == null) continue;

      // recommendUserProfileList에 유저 프로필 정보를 저장함
      addUserProfileToCollection(recommendUserProfileList, user, recommendedUser);
    }

    // 캐시 저장
    redisUtils.saveRecommendUserForDailySurvey(userId.toString(), recommendUserProfileList);
    return recommendUserProfileList;
  }

  public void hideRecommendedUser(User user, Long recommendedUserId){

    Long userId = user.getId();

    if(redisUtils.containsRecommendedUser(userId.toString())){
      List<GetUserProfileResponseDto> responses = redisUtils.getRecommendedUserForCumulativeSurvey(userId.toString());
      removeRecommendedUserFromList(responses, recommendedUserId);
      redisUtils.saveRecommendedUserForCumulativeSurvey(userId.toString(), responses);
    }

    if (redisUtils.containsRecommendUserForDailySurvey(userId.toString())){
      List<GetUserProfileResponseDto> responses = redisUtils.getRecommendUserForDailySurvey(userId.toString());
      removeRecommendedUserFromList(responses, recommendedUserId);
      redisUtils.saveRecommendUserForDailySurvey(userId.toString(), responses);
    }

  }

  public void removeRecommendedUserFromList(List<GetUserProfileResponseDto> responses, Long recommendedUserId){
    for (GetUserProfileResponseDto response : responses){
      if (response.getUserId().equals(recommendedUserId)){
        response.setHide(HIDE_PROFILE_FLAG);
        return;
      }
    }
  }

  public void addUserProfileToCollection(Collection<GetUserProfileResponseDto> collection, User user, User recommendedUser){
    Profile profile = profileRepository.findByUser(recommendedUser)
        .orElseThrow(() -> new RingoException("유저가 프로필을 가지지 않습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR));
    List<String> hashtags = hashtagRepository.findAllByUser(recommendedUser)
        .stream()
        .map(Hashtag::getHashtag)
        .toList();
    float matchingScore = calcMatchScore(user.getId(), recommendedUser.getId());
    collection.add(
        GetUserProfileResponseDto.builder()
            .userId(recommendedUser.getId())
            .matchingScore(matchingScore)
            .age(recommendedUser.getAge())
            .nickname(recommendedUser.getNickname())
            .profileUrl(profile.getImageUrl())
            .hashtags(hashtags)
            .hide(EXPOSE_PROFILE_FLAG)
            .build()
    );
  }
  
  public List<Long> getExcludedUserIdsForRecommendation(Long userId){
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
    List<Long> suspendedUserIds = redisUtils.getSuspendedUsers()
        .stream()
        .map(s -> s.replace("suspension::", ""))
        .map(Long::parseLong)
        .toList();
    // 로그아웃한 유저
    List<Long> logoutUserIds = redisUtils.getLogoutUsers()
        .stream()
        .map(s -> s.replace("logoutUser::", ""))
        .map(Long::parseLong)
        .toList();

    Set<Long> excludedUserId = new HashSet<>();
    excludedUserId.add(userId);
    excludedUserId.addAll(blockedFriendUserIds);
    excludedUserId.addAll(dormantUserIds);
    excludedUserId.addAll(blockedUserIds);
    excludedUserId.addAll(suspendedUserIds);
    excludedUserId.addAll(logoutUserIds);
    
    return new ArrayList<>(excludedUserId);
  }
  
  public List<User> convertIdListToUserList(List<Long> idList){
    return userRepository.findAllByIdIn(idList);
  }

  public boolean isMatch(Float matchingScore){
    return matchingScore > LIMIT_OF_MATCHING_SCORE;
  }

  public float calcMatchScore(Long user1Id, Long user2Id){
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

    setHashtagInProfileDtoList(requestedUserProfileDtoList);

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

    setHashtagInProfileDtoList(requestUserProfileDtoList);

    return requestUserProfileDtoList;
  }

  private void setHashtagInProfileDtoList(List<GetUserProfileResponseDto> profiles){
    profiles.forEach(profile -> {
      User user = userRepository.findById(profile.getUserId()).orElse(null);
      if (user == null) return;
      List<String> hashtags = hashtagRepository.findAllByUser(user)
          .stream()
          .map(Hashtag::getHashtag)
          .toList();
      profile.setHashtags(hashtags);
    });
  }

  @Transactional
  public void deleteMatching(Long matchingId, User user){

    Matching match = matchingRepository.findById(matchingId)
        .orElseThrow(() -> new RingoException("해당 매칭을 찾을 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));

    Long requestedUserId = match.getRequestedUser().getId();
    Long requestUserId = match.getRequestUser().getId();
    if (!(requestedUserId.equals(user.getId()) || requestUserId.equals(user.getId()))){
      log.error("authUserId={}, step=잘못된_유저_요청, status=FAILED", user.getId());
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
      log.error("authUserId={}, userId={}, step=잘못된_유저_요청, status=FAILED", user.getId(), requestUser.getId());
      throw new RingoException("요청 메세지를 저장 및 수정할 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.BAD_REQUEST);
    }
    matching.setMatchingStatus(MatchingStatus.PENDING);
    matching.setMatchingRequestMessage(dto.message());

    // 로그에 저장
    UserMatchingLog log = UserMatchingLog.builder()
        .userId(requestUser.getId())
        .matchingId(matching.getId())
        .status(MatchingStatus.PENDING)
        .gender(requestUser.getGender())
        .build();

    matchingRepository.save(matching);
    userMatchingLogRepository.save(log);

    // 유저 알림
    fcmService.sendFcmNotification(matching.getRequestedUser(), "누군가 매칭을 요청했어요", null);
  }

  public String getMatchingRequestMessage(Long matchingId, User user){

    Matching match = matchingRepository.findById(matchingId)
        .orElseThrow(() -> new RingoException("해당 매칭을 찾을 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));

    Long requestedUserId = match.getRequestedUser().getId();
    Long requestUserId = match.getRequestUser().getId();
    if (!(requestedUserId.equals(user.getId()) || requestUserId.equals(user.getId()))){
      log.error("authUserId={}, step=잘못된_유저_요청, status=FAILED", user.getId());
      throw new RingoException("해당 매칭의 요청 메세지를 확인할 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.BAD_REQUEST);
    }

    return match.getMatchingRequestMessage();
  }
}
