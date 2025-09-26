package com.lingo.lingoproject.match;

import com.lingo.lingoproject.domain.Matching;
import com.lingo.lingoproject.domain.UserEntity;
import com.lingo.lingoproject.domain.enums.Gender;
import com.lingo.lingoproject.domain.enums.MatchingStatus;
import com.lingo.lingoproject.match.dto.MatchingRequestDto;
import com.lingo.lingoproject.repository.MatchingsRepository;
import com.lingo.lingoproject.repository.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MatchService {

  private final UserRepository userRepository;
  private final MatchingsRepository  matchingsRepository;

  public void matchRequest(MatchingRequestDto dto){
    UserEntity requestedUser = userRepository.findById(dto.requestedId())
        .orElseThrow(() -> new IllegalArgumentException("User not found"));
    UserEntity requestUser = userRepository.findById(dto.requestId())
        .orElseThrow(() -> new IllegalArgumentException("User not found"));
    Matching matching = Matching.builder()
        .requestedUser(requestedUser)
        .requestUser(requestUser)
        .matchingStatus(MatchingStatus.PENDING)
        .build();
    matchingsRepository.save(matching);
  }

  @Cacheable(key = "#userId", value = "Recommend", cacheManager = "cacheManager")
  public Set<Long> recommend(Long userId){
    Set<Long> rtnSet = new HashSet<>();
    int count =  0;
    int setSize = 0;
    UserEntity userEntity = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("User not found"));
    // 이성을 추천하도록
    Gender gender = null;
    if(userEntity.getGender().equals(Gender.MALE)){
      gender = Gender.FEMALE;
    }else{
      gender = Gender.MALE;
    }
    while(setSize < 10 && count < 10){
      List<UserEntity> randUserId = userRepository.findRandomUsers();
      for(UserEntity user : randUserId){
        if(!user.getGender().equals(gender)){
          continue;
        }
        // 7명은 매칭이 가능하도록
        if(setSize <= 7){
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
}
