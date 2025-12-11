package com.lingo.lingoproject.match;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.lingo.lingoproject.chat.ChatService;
import com.lingo.lingoproject.chat.dto.CreateChatroomRequestDto;
import com.lingo.lingoproject.domain.AnsweredSurvey;
import com.lingo.lingoproject.domain.BlockedUser;
import com.lingo.lingoproject.domain.DormantAccount;
import com.lingo.lingoproject.domain.Hashtag;
import com.lingo.lingoproject.domain.Matching;
import com.lingo.lingoproject.domain.Profile;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.domain.enums.ChatType;
import com.lingo.lingoproject.domain.enums.MatchingStatus;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.match.dto.GetUserProfileResponseDto;
import com.lingo.lingoproject.match.dto.MatchScoreResultInterface;
import com.lingo.lingoproject.match.dto.MatchingRequestDto;
import com.lingo.lingoproject.match.dto.SaveMatchingRequestMessageRequestDto;
import com.lingo.lingoproject.repository.AnsweredSurveyRepository;
import com.lingo.lingoproject.repository.BlockedFriendRepository;
import com.lingo.lingoproject.repository.BlockedUserRepository;
import com.lingo.lingoproject.repository.DormantAccountRepository;
import com.lingo.lingoproject.repository.FcmTokenRepository;
import com.lingo.lingoproject.repository.HashtagRepository;
import com.lingo.lingoproject.repository.MatchingRepository;
import com.lingo.lingoproject.repository.ProfileRepository;
import com.lingo.lingoproject.repository.UserRepository;
import com.lingo.lingoproject.repository.impl.UserRepositoryImpl;
import com.lingo.lingoproject.utils.RedisUtils;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
  private final FcmTokenRepository fcmTokenRepository;
  private final UserRepositoryImpl userRepositoryImpl;

  private final int MAX_PROFILE_RECOMMENDATION_SIZE = 4;
  private final int MAX_NUMBER_OF_LOOP = 10;
  private final int MAX_DAILY_RECOMMENDATION_SIZE = 4;
  private final float LIMIT_OF_MATCHING_SCORE = 0.6f;

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
        .orElseThrow(() -> new RingoException("매칭을 요청한 유저를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST));
    User requestedUser = userRepository.findById(dto.requestedId())
        .orElseThrow(() -> new RingoException("매칭을 요청 받은 유저를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST));

    // 매칭 점수 계산
    Float matchingScore = calcMatchScore(requestedUser.getId(), requestUser.getId());
    log.info("requestUserId={}, requestedUserId={}, matchingScore={}, step=매칭요청, status=SUCCESS", requestUser.getId(), requestedUser.getId(), matchingScore);
    if(!isMatch(matchingScore)){
      return null;
    }

    // 매칭 내용 저장
    Matching matching = Matching.builder()
        .requestedUser(requestedUser)
        .requestUser(requestUser)
        .matchingStatus(MatchingStatus.PRE_REQUESTED)
        .matchingScore(calcMatchScore(requestedUser.getId(), requestUser.getId()))
        .build();
    return matchingRepository.save(matching);
  }

  /**
   * 연결 수락시 채팅방 생성 및 status를 ACCEPTED로 변경
   * 거절시 status만 REJECTED로 변경
   * @param decision
   * @param matchingId
   */
  public void responseToRequest(String decision, Long matchingId, User user) {

    Matching matching = matchingRepository.findById(matchingId)
        .orElseThrow(() -> new RingoException("적절하지 않은 매칭 id 입니다.", HttpStatus.BAD_REQUEST));

    Long requestedUserId = matching.getRequestedUser().getId();
    if (!requestedUserId.equals(user.getId())) {
      log.error("authUserId={}, userId={}, step=잘못된_유저_요청, status=FAILED", user.getId(), requestedUserId);
      throw new RingoException("매칭 수락 여부를 결정할 권한이 없습니다.", HttpStatus.FORBIDDEN);
    }

    switch (decision) {
      case "ACCEPTED" -> handleAcceptedMatching(matching);
      case "REJECTED" -> matching.setMatchingStatus(MatchingStatus.REJECTED);
      default -> throw new RingoException("decision 값이 적절하지 않습니다.", HttpStatus.BAD_REQUEST);
    }

    matchingRepository.save(matching);
  }

  private void handleAcceptedMatching(Matching matching){

    // 매칭 수락으로 변경
    matching.setMatchingStatus(MatchingStatus.ACCEPTED);

    // 채팅방 생성
    chatService.createChatroom(
        new CreateChatroomRequestDto(
            matching.getRequestedUser().getId(),
            matching.getRequestUser().getId(),
            ChatType.USER.toString()
        )
    );

    // fcm 요청 전송
    sendFcmNotification(matching.getRequestUser(), "누군가 요청을 수락했어요");
  }

  private void sendFcmNotification(User requestUser, String title) {
    fcmTokenRepository.findByUser(requestUser).ifPresent(token -> {
      Message message = Message.builder()
          .setNotification(
              Notification.builder()
                  .setTitle(title)
                  .setImage("ImageUrl")
                  .build()
          )
          .build();
      try {
        FirebaseMessaging.getInstance().send(message);
      }catch (Exception e){
        log.error("step=FCM_알림_전송_실패, requestUserId={}, title={}, status=FAILED", requestUser.getId(), title, e);
        throw new RingoException("fcm 메세지를 보내는데 실패하였습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
      }
    });
  }

  @Transactional
  public List<GetUserProfileResponseDto> recommend(Long userId){

    // 캐시 조회
    if(redisUtils.containsRecommendUser(userId.toString())){
      List<GetUserProfileResponseDto> responses = redisUtils.getRecommendUser(userId.toString());
      if (responses.size() == MAX_PROFILE_RECOMMENDATION_SIZE) return responses;
    }

    // 유저 조회
    User currentUser = userRepository.findById(userId)
        .orElseThrow(() -> new RingoException("User not found", HttpStatus.BAD_REQUEST));

    Set<GetUserProfileResponseDto> recommendUserProfileSet = new HashSet<>();

    List<Long> excludedUserIds = getExcludedUserIdsForRecommendation(currentUser.getId());

    int whileLoopCount =  0;
    int recommendationSize = 0;

    while(recommendationSize < MAX_PROFILE_RECOMMENDATION_SIZE && whileLoopCount < MAX_NUMBER_OF_LOOP){
      List<User> randomCandidates = userRepositoryImpl.findRandomRecommendationCandidates(currentUser.getGender(), excludedUserIds);
      for(User candidateUser : randomCandidates){
        if(recommendationSize >= MAX_PROFILE_RECOMMENDATION_SIZE) break;
        Float matchingScore = calcMatchScore(userId, candidateUser.getId());
        if(isMatch(matchingScore)){
          addUserProfileToCollection(recommendUserProfileSet, candidateUser);
          recommendationSize++;
        }
      }
      whileLoopCount++;
    }

    List<GetUserProfileResponseDto> recommendUserList = new ArrayList<>(recommendUserProfileSet);

    // 캐시 저장
    redisUtils.saveRecommendUser(userId.toString(), recommendUserList);

    return recommendUserList;
  }

  public List<GetUserProfileResponseDto> recommendUserByDailySurvey(User user){

    Long userId = user.getId();

    //캐시 조회
    if (redisUtils.containsRecommendUserForDailySurvey(userId.toString())){
      List<GetUserProfileResponseDto> responses = redisUtils.getRecommendUserForDailySurvey(userId.toString());
      if (responses.size() == MAX_DAILY_RECOMMENDATION_SIZE) return responses;
    }

    // 오늘 응답한 설문 조회
    List<AnsweredSurvey> todayAnsweredSurveys = answeredSurveyRepository.findAllByUserAndCreatedAtAfter(
        user,
        LocalDate.now().atStartOfDay()
    );

    // 만약 설문을 완료하지 않았다면 추천받을 수 없음.
    if (todayAnsweredSurveys.isEmpty()){
      return null;
    }

    // 4개의 설문에 대해서만 추천해줌
    Random randomGenerator = new Random(System.currentTimeMillis());
    while(todayAnsweredSurveys.size() > MAX_DAILY_RECOMMENDATION_SIZE){
      int index = randomGenerator.nextInt(todayAnsweredSurveys.size());
      todayAnsweredSurveys.remove(index);
    }

    // 유저와 같은 응답을 한 이성을 추천해줌
    List<GetUserProfileResponseDto> recommendUserProfileList = new ArrayList<>();
    for (AnsweredSurvey todayAnsweredSurvey : todayAnsweredSurveys) {
      List<User> excludedUsers = convertIdListToUserList(getExcludedUserIdsForRecommendation(user.getId()));
      List<AnsweredSurvey> matchingAnsweredSurveyList = answeredSurveyRepository.findAllByUserNotInAndAnswerAndSurveyNum(
          excludedUsers,
          todayAnsweredSurvey.getAnswer(),
          todayAnsweredSurvey.getSurveyNum()
      );
      int randomIndex = randomGenerator.nextInt(matchingAnsweredSurveyList.size());
      AnsweredSurvey matchingAnsweredSurvey = matchingAnsweredSurveyList.get(randomIndex);
      User recommendedUser = matchingAnsweredSurvey.getUser();
      addUserProfileToCollection(recommendUserProfileList, recommendedUser);
    }

    // 캐시 저장
    redisUtils.saveRecommendUserForDailySurvey(userId.toString(), recommendUserProfileList);
    return recommendUserProfileList;
  }

  public void addUserProfileToCollection(Collection<GetUserProfileResponseDto> collection, User user){
    Profile profile = profileRepository.findByUser(user)
        .orElseThrow(() -> new RingoException("유저가 프로필을 가지지 않습니다.", HttpStatus.INTERNAL_SERVER_ERROR));
    List<String> hashtags = hashtagRepository.findAllByUser(user)
        .stream()
        .map(Hashtag::getHashtag)
        .toList();
    collection.add(
        GetUserProfileResponseDto.builder()
            .userId(user.getId())
            .age(user.getAge())
            .nickname(user.getNickname())
            .profileUrl(profile.getImageUrl())
            .hashtags(hashtags)
            .build()
    );
  }
  
  public List<Long> getExcludedUserIdsForRecommendation(Long userId){
    // 연락처에 존재하는 유저
    List<Long> blockedFriendUserIds = blockedFriendRepository.findUsersMutuallyBlockedWith(userId)
        .stream()
        .map(User::getId)
        .toList();
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
    List<Long> suspendedUserIds = redisUtils.getSuspendedUser()
        .stream()
        .map(s -> s.replace("suspension::", ""))
        .map(Long::parseLong)
        .toList();

    List<Long> excludedUserId = new ArrayList<>();
    excludedUserId.addAll(blockedFriendUserIds);
    excludedUserId.addAll(dormantUserIds);
    excludedUserId.addAll(blockedUserIds);
    excludedUserId.addAll(suspendedUserIds);
    
    return excludedUserId;
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
        .filter(m -> m.getMatchingStatus() != MatchingStatus.PRE_REQUESTED)
        .map(Matching::getId)
        .toList();

    // 매칭 요청 받은 유저들의 정보 조회
    List<GetUserProfileResponseDto> requestedUserProfileDtoList = profileRepository.getRequestedUserProfilesByMatchingIds(matchingIds);

    // 매칭 요청 받은 유저 객체 조회
    List<User> matchingRequestedUsers = matchings.stream().map(Matching::getRequestedUser).toList();

    // 유저id-해시태크 쌍 조회
    Map<Long, List<String>> userIdToHashtagMap = convertToMapFromHashtags(hashtagRepository.findAllByUserIn(matchingRequestedUsers));

    // dto에 해시태그 값 추가
    requestedUserProfileDtoList.forEach(profile -> profile.setHashtags(userIdToHashtagMap.get(profile.getUserId())));
    return requestedUserProfileDtoList;
  }

  public List<GetUserProfileResponseDto> getUserIdWhoRequestToMe(User requestedUser){
    List<Matching> matchings = matchingRepository.findAllByRequestedUser(requestedUser);

    List<Long> matchingIds = matchings.stream()
        .filter(m ->
              (m.getMatchingStatus() == MatchingStatus.PENDING) ||
              (m.getMatchingStatus() == MatchingStatus.ACCEPTED)
        )
        .map(Matching::getId)
        .toList();

    List<GetUserProfileResponseDto> requestUserProfileDtoList = profileRepository.getRequestUserProfilesByMatchingIds(matchingIds);

    List<User> matchingRequestUsers = matchings.stream().map(Matching::getRequestUser).toList();

    Map<Long, List<String>> userIdToHashtagMap = convertToMapFromHashtags(hashtagRepository.findAllByUserIn(matchingRequestUsers));

    requestUserProfileDtoList.forEach(profile -> profile.setHashtags(userIdToHashtagMap.get(profile.getUserId())));

    return requestUserProfileDtoList;
  }

  public Map<Long, List<String>> convertToMapFromHashtags(List<Hashtag> hashtags){
    return hashtags.stream()
        .collect(Collectors.groupingBy(
            h -> h.getUser().getId(),
            Collectors.mapping(Hashtag::getHashtag, Collectors.toList())
        ));
  }

  @Transactional
  public void deleteMatching(Long matchingId, User user){

    Matching match = matchingRepository.findById(matchingId).orElseThrow(() -> new RingoException("해당 매칭을 찾을 수 없습니다.", HttpStatus.BAD_REQUEST));

    Long requestedUserId = match.getRequestedUser().getId();
    Long requestUserId = match.getRequestUser().getId();
    if (!(requestedUserId.equals(user.getId()) || requestUserId.equals(user.getId()))){
      log.error("authUserId={}, step=잘못된_유저_요청, status=FAILED", user.getId());
      throw new RingoException("매칭을 삭제할 권한이 없습니다.", HttpStatus.BAD_REQUEST);
    }

    matchingRepository.deleteById(matchingId);
  }

  @Transactional
  public void saveMatchingRequestMessage(SaveMatchingRequestMessageRequestDto dto, Long matchingId, User user){

    Matching match = matchingRepository.findById(matchingId)
        .orElseThrow(() -> new RingoException("해당 매칭을 찾을 수 없습니다.", HttpStatus.BAD_REQUEST));

    Long requestUserId = match.getRequestUser().getId();
    if (!requestUserId.equals(user.getId())){
      log.error("authUserId={}, userId={}, step=잘못된_유저_요청, status=FAILED", user.getId(), requestUserId);
      throw new RingoException("요청 메세지를 저장 및 수정할 권한이 없습니다.", HttpStatus.BAD_REQUEST);
    }
    match.setMatchingStatus(MatchingStatus.PENDING);
    match.setMatchingRequestMessage(dto.message());

    matchingRepository.save(match);

    // 유저 알림
    sendFcmNotification(match.getRequestedUser(), "누군가 매칭을 요청했어요");
  }

  public String getMatchingRequestMessage(Long matchingId, User user){

    Matching match = matchingRepository.findById(matchingId)
        .orElseThrow(() -> new RingoException("해당 매칭을 찾을 수 없습니다.", HttpStatus.BAD_REQUEST));

    Long requestedUserId = match.getRequestedUser().getId();
    Long requestUserId = match.getRequestUser().getId();
    if (!(requestedUserId.equals(user.getId()) || requestUserId.equals(user.getId()))){
      log.error("authUserId={}, step=잘못된_유저_요청, status=FAILED", user.getId());
      throw new RingoException("해당 매칭의 요청 메세지를 확인할 권한이 없습니다.", HttpStatus.BAD_REQUEST);
    }

    return match.getMatchingRequestMessage();
  }
}
