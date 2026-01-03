package com.lingo.lingoproject.stats;

import com.lingo.lingoproject.domain.enums.Gender;
import com.lingo.lingoproject.repository.UserAccessLogRepository;
import com.lingo.lingoproject.repository.UserRepository;
import com.lingo.lingoproject.stats.dto.GetDailyNumberOfVisitorRequestDto;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StatService {
  private final UserAccessLogRepository userAccessLogRepository;
  private final UserRepository userRepository;

  public long getTodayNumberOfVisitor(){
    LocalDateTime todayDateTime = LocalDate.now().atStartOfDay();
    return userAccessLogRepository.countByCreateAtAfter(todayDateTime);
  }

  public float getTodayMaleRatioOfVisitor(){
    long todayNumberOfMaleVisitor = userAccessLogRepository.countByCreateAtAndGender(LocalDateTime.now(), Gender.MALE);
    long todayNumberOfVisitor = getTodayNumberOfVisitor();
    return Math.round(((float) todayNumberOfMaleVisitor /todayNumberOfVisitor) * 100 * 10) / 10.0f;
  }

  public List<GetDailyNumberOfVisitorRequestDto> getDailyNumberOfVisitorForWeek(){
    // 시작 날짜 - 6일전
    LocalDate startDate = LocalDate.now().minusDays(6);
    LocalDateTime startDateTime = startDate.atStartOfDay();

    // 날짜 - 접속자 수
    Map<String, Long> userAccessMap = userAccessLogRepository.findAllByCreateAtAfter(startDateTime)
        .stream()
        .collect(Collectors.groupingBy(
            log -> log.getCreateAt().toLocalDate().toString(),
            Collectors.counting()
        ));

    // 날짜 - 회원가입 수
    Map<String, Long> userSignupMap = userRepository.findAllByCreatedAtAfter(startDateTime)
        .stream()
        .collect(Collectors.groupingBy(
            user -> user.getCreatedAt().toLocalDate().toString(),
            Collectors.counting()
        ));

    List<GetDailyNumberOfVisitorRequestDto> result = new ArrayList<>();
    for (int i = 0; i < 7; i++){
      LocalDate currentDate = startDate.plusDays(i);
      long dailyNumberOfVisitor = userAccessMap.getOrDefault(currentDate.toString(), 0L);
      long dailyNumberOfSignup = userSignupMap.getOrDefault(currentDate.toString(), 0L);
      result.add(new GetDailyNumberOfVisitorRequestDto(
          currentDate.toString(),
          (int) dailyNumberOfVisitor,
          (int) dailyNumberOfSignup));
    }
    return result;
  }
}
