package com.lingo.lingoproject.match;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.lingo.lingoproject.chat.ChatService;
import com.lingo.lingoproject.chat.dto.CreateChatroomDto;
import com.lingo.lingoproject.domain.AnsweredSurvey;
import com.lingo.lingoproject.domain.BlockedUser;
import com.lingo.lingoproject.domain.DormantAccount;
import com.lingo.lingoproject.domain.FcmToken;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
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

  private final int MAX_RECOMMEND_PROFILE_SIZE = 10;
  private final int MAX_NUMBER_OF_LOOP = 10;
  private final int NUMBER_OF_ENABLE_MATCHING = 7;
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
    User requestedUser = userRepository.findById(dto.requestedId())
        .orElseThrow(() -> new RingoException("User not found", HttpStatus.BAD_REQUEST));
    User requestUser = userRepository.findById(dto.requestId())
        .orElseThrow(() -> new RingoException("User not found", HttpStatus.BAD_REQUEST));
    if(!isMatch(calcMatchScore(requestedUser.getId(), requestUser.getId()))){
      throw new RingoException("연결 적합도가 기준 미만입니다.", HttpStatus.NOT_ACCEPTABLE);
    }
    Matching matching = Matching.builder()
        .requestedUser(requestedUser)
        .requestUser(requestUser)
        .matchingStatus(MatchingStatus.PENDING)
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
  public void responseToRequest(String decision, Long matchingId)
      throws FirebaseMessagingException {
    Matching matching = matchingRepository.findById(matchingId)
        .orElseThrow(() -> new RingoException("적절하지 않은 매칭 id 입니다.", HttpStatus.BAD_REQUEST));
    MatchingStatus status = null;
    if(decision.equals(MatchingStatus.ACCEPTED.toString())){
      status = MatchingStatus.ACCEPTED;
      chatService.createChatroom(
          new CreateChatroomDto(matching.getRequestedUser().getId(), matching.getRequestUser().getId(), ChatType.USER.toString()));
    }
    else if(decision.equals(MatchingStatus.REJECTED.toString())){
      status = MatchingStatus.REJECTED;
    }
    else{
      throw new RingoException("decision 값이 적절하지 않습니다.", HttpStatus.BAD_REQUEST);
    }
    matching.setMatchingStatus(status);
    matchingRepository.save(matching);

    if (status == MatchingStatus.ACCEPTED) {
      // 매칭 수락을 하면 fcm 알림 전송
      User user = matching.getRequestUser();
      FcmToken token = fcmTokenRepository.findByUser(user).orElse(null);
      if (token != null){
        Message message = Message.builder()
            .setNotification(Notification.builder()
                .setTitle("누군가 요청을 수락했어요")
                .setImage("ImageUrl")
                .build())
            .build();
        FirebaseMessaging.getInstance().send(message);
      }
    }
  }

  @Transactional
  public List<GetUserProfileResponseDto> recommend(Long userId){

    if(redisUtils.containsRecommendUser(userId.toString())){
      return redisUtils.getRecommendUser(userId.toString());
    }

    Set<GetUserProfileResponseDto> rtnSet = new HashSet<>();
    int count =  0;
    int setSize = 0;
    User userEntity = userRepository.findById(userId)
        .orElseThrow(() -> new RingoException("User not found", HttpStatus.BAD_REQUEST));
    List<Long> banUserId = getBannedRecommendedUserList(userEntity.getId());

    /**
     * setSize는 rtnSet의 크기로 추천이성 정보의 개수이다.
     * setSize의 크기가 10 이상이거나 반복문을 10번 돌면 반복문을 빠져나온다.
     *
     * findRandomUsers는 banIds를 제외한 이성친구를 무작위로 추천한다.
     * setSize가 7이하 일때는 매칭이 가능한 사람을 추가하고 8이상일 때는 매칭이 불가능한 이성을 추천한다.
     */
    while(setSize < MAX_RECOMMEND_PROFILE_SIZE && count < MAX_NUMBER_OF_LOOP){
      List<User> randUsers = userRepositoryImpl.findRandomOppositeGenderAndNotBannedFriend(userEntity.getGender(), banUserId);
      for(User randUser : randUsers){
        // setSize가 10 이상일 때는 while & for 반복문이 종료된다.
        if(setSize >= MAX_RECOMMEND_PROFILE_SIZE) break;
        Float matchingScore = calcMatchScore(userId, randUser.getId());
        /*
        // 7명은 매칭이 가능하도록
        if((setSize < NUMBER_OF_ENABLE_MATCHING && isMatch(matchingScore)) ||
            (setSize >= NUMBER_OF_ENABLE_MATCHING && !isMatch(matchingScore))){
         */
        if(isMatch(matchingScore)){
          Profile profile = profileRepository.findByUser(randUser)
              .orElseThrow(() -> new RingoException("유저가 프로필을 가지지 않습니다.", HttpStatus.INTERNAL_SERVER_ERROR));
          List<String> hashtags = hashtagRepository.findAllByUser(randUser)
              .stream()
              .map(Hashtag::getHashtag)
              .toList();
          rtnSet.add(
              GetUserProfileResponseDto.builder()
                  .userId(randUser.getId())
                  .age(randUser.getAge())
                  .nickname(randUser.getNickname())
                  .profileUrl(profile.getImageUrl())
                  .hashtags(hashtags)
                  .build()
          );
          setSize++;
        }
      }
      count++;
    }

    List<GetUserProfileResponseDto> recommendUserList = new ArrayList<>(rtnSet);
    redisUtils.saveRecommendUser(userId.toString(), recommendUserList);
    return recommendUserList;
  }

  public List<GetUserProfileResponseDto> recommendUserForDailySurvey(Long userId){
    if (redisUtils.containsRecommendUserForDailySurvey(userId.toString())){
      return redisUtils.getRecommendUserForDailySurvey(userId.toString());
    }
    User user = userRepository.findById(userId).orElseThrow(() -> new RingoException("유저를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST));
    if (!isCompleteTodaySurvey(user)){
      return null;
    }
    List<AnsweredSurvey> answeredSurveys = answeredSurveyRepository.findAllByUserAndCreatedAtBetween(
        user,
        LocalDate.now().atStartOfDay(),
        LocalDateTime.now()
    );
    Random random = new Random(System.currentTimeMillis());

    // 4명만 추천해줌
    while(answeredSurveys.size() > 4){
      int index = random.nextInt(answeredSurveys.size());
      answeredSurveys.remove(index);
    }
    List<GetUserProfileResponseDto> recommendUserList = new ArrayList<>();
    for (AnsweredSurvey answeredSurvey : answeredSurveys) {
      List<User> bannedUsers = convertIdListToUserList(getBannedRecommendedUserList(user.getId()));
      List<AnsweredSurvey> sameAnsweredSurveyList = answeredSurveyRepository.findAllByUserNotInAndAnswerAndSurveyNum(
          bannedUsers,
          answeredSurvey.getAnswer(),
          answeredSurvey.getSurveyNum()
      );
      AnsweredSurvey sameAnsweredSurvey = sameAnsweredSurveyList.get(random.nextInt(sameAnsweredSurveyList.size()));
      User recommendedUser = sameAnsweredSurvey.getUser();
      Profile profile = profileRepository.findByUser(recommendedUser)
          .orElseThrow(() -> new RingoException("유저가 프로필을 가지지 않습니다.", HttpStatus.INTERNAL_SERVER_ERROR));
      List<String> hashtags = hashtagRepository.findAllByUser(recommendedUser)
          .stream()
          .map(Hashtag::getHashtag)
          .toList();
      recommendUserList.add(
          GetUserProfileResponseDto.builder()
              .userId(recommendedUser.getId())
              .age(recommendedUser.getAge())
              .nickname(recommendedUser.getNickname())
              .profileUrl(profile.getImageUrl())
              .hashtags(hashtags)
              .build()
      );
    }
    return recommendUserList;
  }
  
  public List<Long> getBannedRecommendedUserList(Long userId){
    List<Long> blockedFriendIds = blockedFriendRepository.findBlockedFriends(userId)
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
    List<Long> suspendedUserId = redisUtils.getSuspendedUser()
        .stream()
        .map(s -> s.replace("suspension::", ""))
        .map(Long::parseLong)
        .toList();

    List<Long> banUserId = new ArrayList<>();
    banUserId.addAll(blockedFriendIds);
    banUserId.addAll(dormantUserIds);
    banUserId.addAll(blockedUserIds);
    banUserId.addAll(suspendedUserId);
    
    return banUserId;
  }
  
  public List<User> convertIdListToUserList(List<Long> idList){
    return userRepository.findAllByIdIn(idList);
  }

  public boolean isCompleteTodaySurvey(User user){
    boolean isComplete = answeredSurveyRepository.existsByUserAndCreatedAtBetween(
        user,
        LocalDate.now().atStartOfDay(),
        LocalDateTime.now()
    );
    return isComplete;
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
  public List<GetUserProfileResponseDto> getUserIdRequested(Long userId){
    User requestUser = userRepository.findById(userId)
            .orElseThrow(() -> new RingoException("User not found", HttpStatus.BAD_REQUEST));
    List<Matching> matchings = matchingRepository.findByRequestUserAndMatchingStatus(requestUser, MatchingStatus.PENDING);
    List<Long> matchingIds = matchings.stream()
        .map(Matching::getId)
        .toList();
    /**
     * userId 기반으로 user의 id, age, gender, nickname, profileUrl, hashtags
     * 를 조회하는 함수
     */
    List<GetUserProfileResponseDto> profiles = profileRepository.getRequestedUserProfilesByMatchingIds(matchingIds);

    List<User> matchingRequestedUsers = matchings.stream()
        .map(Matching::getRequestedUser)
        .toList();
    Map<Long, List<String>> hashtagMap = convertToMapFromHashtags(hashtagRepository.findAllByUserIn(matchingRequestedUsers));
    profiles.forEach(profile -> profile.setHashtags(hashtagMap.get(profile.getUserId())));
    return profiles;
  }
  public List<GetUserProfileResponseDto> getUserIdRequests(Long userId){
    User requestedUser = userRepository.findById(userId)
        .orElseThrow(() -> new RingoException("User not found", HttpStatus.BAD_REQUEST));
    List<Matching> matchings = matchingRepository.findByRequestedUserAndMatchingStatus(requestedUser, MatchingStatus.PENDING);

    // id, age, gender, nickname, profileUrl 조회
    List<Long> matchingIds = matchings.stream()
        .map(Matching::getId)
        .toList();
    List<GetUserProfileResponseDto> profiles = profileRepository.getRequestUserProfilesByMatchingIds(matchingIds);

    // hashtags 조회
    List<User> matchingRequestUsers = matchings.stream()
        .map(Matching::getRequestUser)
        .toList();
    Map<Long, List<String>> hashtagMap = convertToMapFromHashtags(hashtagRepository.findAllByUserIn(matchingRequestUsers));
    profiles.forEach(profile -> profile.setHashtags(hashtagMap.get(profile.getUserId())));

    return profiles;
  }

  public Map<Long, List<String>> convertToMapFromHashtags(List<Hashtag> hashtags){
    Map<Long, List<String>> hashtagMap = new HashMap<>();
    for(Hashtag hashtag : hashtags){
      Long userId = hashtag.getUser().getId();
      hashtagMap.putIfAbsent(userId, new ArrayList<>());
      hashtagMap.get(userId).add(hashtag.getHashtag());
    }
    return hashtagMap;
  }

  public void deleteMatching(Long id){
    matchingRepository.deleteById(id);
  }

  public void saveMatchingRequestMessage(SaveMatchingRequestMessageRequestDto dto){
    Matching match = matchingRepository.findById(dto.matchingId())
        .orElseThrow(() -> new RingoException("해당 매칭을 찾을 수 없습니다.", HttpStatus.BAD_REQUEST));
    match.setMatchingRequestMessage(dto.message());
    matchingRepository.save(match);
  }

  public String getMatchingRequestMessage(Long matchingId){
    Matching match = matchingRepository.findById(matchingId)
        .orElseThrow(() -> new RingoException("해당 매칭을 찾을 수 없습니다.", HttpStatus.BAD_REQUEST));
    return match.getMatchingRequestMessage();
  }
}
