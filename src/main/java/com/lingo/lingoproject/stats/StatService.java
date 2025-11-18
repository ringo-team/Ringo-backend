package com.lingo.lingoproject.stats;

import com.lingo.lingoproject.domain.enums.Gender;
import com.lingo.lingoproject.repository.UserAccessLogRepository;
import com.lingo.lingoproject.repository.UserRepository;
import com.lingo.lingoproject.stats.dto.GetDailyNumberOfVisitorRequestDto;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StatService {
  private final UserAccessLogRepository userAccessLogRepository;
  private final UserRepository userRepository;

  public long getTodayNumberOfVisitor(){
    LocalDateTime todayDateTime = LocalDate.now().atStartOfDay();
    return userAccessLogRepository.countByCreateAtBetween(todayDateTime, LocalDateTime.now());
  }

  public float getTodayMaleRatioOfVisitor(){
    long todayNumberOfMaleVisitor = userAccessLogRepository.countByCreateAtAndGender(LocalDateTime.now(), Gender.MALE);
    long todayNumberOfVisitor = getTodayNumberOfVisitor();
    return Math.round(((float) todayNumberOfMaleVisitor /todayNumberOfVisitor) * 100 * 10) / 10.0f;
  }

  public List<GetDailyNumberOfVisitorRequestDto> getDailyNumberOfVisitor(){
    LocalDate startDate = LocalDate.now().minusDays(6);
    LocalDateTime startDateTime = startDate.atStartOfDay();

    Map<String, Integer> userAccessMap = new HashMap<>();
    userAccessLogRepository.findAllByCreateAtBetween(startDateTime, LocalDateTime.now())
        .forEach(element -> {
          String createDate = element.getCreateAt().toLocalDate().toString();
          userAccessMap.putIfAbsent(createDate, 0);
          userAccessMap.put(createDate, userAccessMap.get(createDate) + 1);
        });

    Map<String, Integer> userSignupMap = new HashMap<>();
    userRepository.findAllByCreatedAtBetween(startDateTime, LocalDateTime.now())
        .forEach(element -> {
          String createDate = element.getCreatedAt().toLocalDate().toString();
          userSignupMap.putIfAbsent(createDate, 0);
          userSignupMap.put(createDate, userSignupMap.get(createDate) + 1);
        });

    List<GetDailyNumberOfVisitorRequestDto> result = new ArrayList<>();
    for (int i = 0; i < 7; i++){
      LocalDate currentDate = startDate.plusDays(i);
      int dailyNumberOfVisitor = userAccessMap.getOrDefault(currentDate.toString(), 0);
      int dailyNumberOfSignup = userSignupMap.getOrDefault(currentDate.toString(), 0);
      result.add(new GetDailyNumberOfVisitorRequestDto(currentDate.toString(), dailyNumberOfVisitor, dailyNumberOfSignup));
    }
    return result;
  }
}
