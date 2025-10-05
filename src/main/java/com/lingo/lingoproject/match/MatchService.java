package com.lingo.lingoproject.match;

import com.lingo.lingoproject.domain.DormantAccount;
import com.lingo.lingoproject.domain.Matching;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.domain.enums.Gender;
import com.lingo.lingoproject.domain.enums.MatchingStatus;
import com.lingo.lingoproject.match.dto.MatchingRequestDto;
import com.lingo.lingoproject.repository.BlockedFriendRepository;
import com.lingo.lingoproject.repository.DormantAccountRepository;
import com.lingo.lingoproject.repository.MatchingRepository;
import com.lingo.lingoproject.repository.UserRepository;
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

  @Cacheable(key = "#userId", value = "Recommend", cacheManager = "cacheManager")
  public Set<Long> recommend(Long userId){
    Set<Long> rtnSet = new HashSet<>();
    int count =  0;
    int setSize = 0;
    User userEntity = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("User not found"));
    // block 하거나 된 사람은 추천하지 않도록
    List<Long> blockedFriendIds = blockedFriendRepository.findBlockedFriends(userId)
        .stream()
        .map(User::getId)
        .toList();
    List<Long> dormantUserIds = dormantAccountRepository.findAll()
        .stream()
        .map(DormantAccount::getUser)
        .map(User::getId)
        .toList();
    // 이성을 추천하도록
    Gender gender = null;
    if(userEntity.getGender().equals(Gender.MALE)){
      gender = Gender.MALE;
    }else{
      gender = Gender.FEMALE;
    }
    while(setSize < 10 && count < 10){
      List<User> randUsers = userRepository.findRandomUsers();
      for(User user : randUsers){
        // 성별이 같은 유저거나 블락된 유저, 휴면 계정의 경우 pass
        if(user.getGender().equals(gender)){
          continue;
        }
        if(blockedFriendIds.contains(user.getId())){
          continue;
        }
        if(dormantUserIds.contains(user.getId())){
          continue;
        }
        // 7명은 매칭이 가능하도록
        if(setSize <= 6){
          if (!userId.equals(user.getId()) && isMatch(userId, user.getId())) {
            rtnSet.add(user.getId());
            setSize++;
          }
        }
        else if(setSize <= 9){
          if (!userId.equals(user.getId()) && !isMatch(userId, user.getId())) {
            rtnSet.add(user.getId());
            setSize++;
          }
        }
        else break;
      }
      count++;
    }
    return rtnSet;
  }

  public boolean isMatch(Long user1, Long user2){
    return true;
  }

  public List<Long> getUserIdRequested(Long userId){
    User requestUser = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    return matchingRepository.findByRequestUserAndMatchingStatus(requestUser, MatchingStatus.PENDING)
        .stream()
        .map(Matching::getRequestedUser)
        .map(User::getId)
        .toList();
  }
  public List<Long> getUserIdRequests(Long userId){
    User requestedUser = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("User not found"));
    return matchingRepository.findByRequestedUserAndMatchingStatus(requestedUser, MatchingStatus.PENDING)
        .stream()
        .map(Matching::getRequestUser)
        .map(User::getId)
        .toList();
  }

  public void deleteMatching(Long id){
    matchingRepository.deleteById(id);
  }
}
