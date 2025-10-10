package com.lingo.lingoproject.match;

import com.lingo.lingoproject.domain.DormantAccount;
import com.lingo.lingoproject.domain.Matching;
import com.lingo.lingoproject.domain.Profile;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.domain.enums.MatchingStatus;
import com.lingo.lingoproject.match.dto.GetUserProfileResponseDto;
import com.lingo.lingoproject.match.dto.MatchingRequestDto;
import com.lingo.lingoproject.repository.BlockedFriendRepository;
import com.lingo.lingoproject.repository.DormantAccountRepository;
import com.lingo.lingoproject.repository.MatchingRepository;
import com.lingo.lingoproject.repository.ProfileRepository;
import com.lingo.lingoproject.repository.UserRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
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

  public Matching matchRequest(MatchingRequestDto dto){
    User requestedUser = userRepository.findById(dto.requestedId())
        .orElseThrow(() -> new IllegalArgumentException("User not found"));
    User requestUser = userRepository.findById(dto.requestId())
        .orElseThrow(() -> new IllegalArgumentException("User not found"));
    Matching matching = Matching.builder()
        .requestedUser(requestedUser)
        .requestUser(requestUser)
        .matchingStatus(MatchingStatus.PENDING)
        .build();
    return matchingRepository.save(matching);
  }

  public void responseToRequest(String decision, Long matchingId){
    MatchingStatus status = null;
    if(decision.equals(MatchingStatus.ACCEPTED.toString())){
      status = MatchingStatus.ACCEPTED;
    }
    else if(decision.equals(MatchingStatus.REJECTED.toString())){
      status = MatchingStatus.REJECTED;
    }
    else{
      throw new IllegalArgumentException("decision 값이 적절하지 않습니다.");
    }
    Matching matching = matchingRepository.findById(matchingId)
        .orElseThrow(() -> new IllegalArgumentException("적절하지 않은 매칭 id 입니다."));
    matching.setMatchingStatus(status);
    matchingRepository.save(matching);
  }

  @Cacheable(key = "#userId", value = "recommend", cacheManager = "cacheManager")
  public List<GetUserProfileResponseDto> recommend(Long userId){
    Set<GetUserProfileResponseDto> rtnSet = new HashSet<>();
    int count =  0;
    int setSize = 0;
    User userEntity = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("User not found"));
    // 자신의 연락처에 존재하는 유저와 자신을 연락처로 포함하고 있는 유저
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

    List<Long> banIds = new ArrayList<>();
    banIds.addAll(blockedFriendIds);
    banIds.addAll(dormantUserIds);

    /**
     * setSize는 rtnSet의 크기로 추천이성 정보의 개수이다.
     * setSize의 크기가 10 이상이거나 반복문을 10번 돌면 반복문을 빠져나온다.
     *
     * findRandomUsers는 banIds를 제외한 이성친구를 무작위로 추천한다.
     * setSize가 7이하 일때는 매칭이 가능한 사람을 추가하고 8이상일 때는 매칭이 불가능한 이성을 추천한다.
     */
    while(setSize < 10 && count < 10){
      List<User> randUsers = userRepository.findRandomUsers(userEntity.getGender(), banIds);
      for(User randUser : randUsers){
        // setSize가 10 이상일 때는 while & for 반복문이 종료된다.
        if(setSize >= 10) break;
        // 7명은 매칭이 가능하도록
        if((setSize < 7 && isMatch(userId, randUser.getId())) || (setSize >= 7 && !isMatch(userId, randUser.getId()))){
          Profile profile = profileRepository.findByUser(randUser);
          rtnSet.add(
              GetUserProfileResponseDto.builder()
                  .userId(randUser.getId())
                  .age(randUser.getAge())
                  .nickname(randUser.getNickname())
                  .profileUrl(profile.getImageUrl())
                  .build()
          );
          setSize++;
        }
      }
      count++;
    }
    return new ArrayList<>(rtnSet);
  }

  public boolean isMatch(Long user1, Long user2){
    return true;
  }

  public List<GetUserProfileResponseDto> getUserIdRequested(Long userId){
    User requestUser = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    List<Long> userIds = matchingRepository.findByRequestUserAndMatchingStatus(requestUser, MatchingStatus.PENDING)
        .stream()
        .map(Matching::getRequestedUser)
        .map(User::getId)
        .toList();
    /**
     * userId 기반으로 user의 id, age, gender, nickname, profileUrl
     * 을 조회하는 함수
     */
    return profileRepository.getUserProfilesByUserIds(userIds);
  }
  public List<GetUserProfileResponseDto> getUserIdRequests(Long userId){
    User requestedUser = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("User not found"));
    List<Long> userIds = matchingRepository.findByRequestedUserAndMatchingStatus(requestedUser, MatchingStatus.PENDING)
        .stream()
        .map(Matching::getRequestUser)
        .map(User::getId)
        .toList();
    return profileRepository.getUserProfilesByUserIds(userIds);
  }

  public void deleteMatching(Long id){
    matchingRepository.deleteById(id);
  }
}
