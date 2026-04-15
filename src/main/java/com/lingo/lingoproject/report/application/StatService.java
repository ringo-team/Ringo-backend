package com.lingo.lingoproject.report.application;

import com.lingo.lingoproject.shared.domain.model.Gender;
import com.lingo.lingoproject.shared.infrastructure.persistence.UserAccessLogRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.UserRepository;
import com.lingo.lingoproject.report.presentation.dto.stats.GetDailyNumberOfVisitorRequestDto;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 관리자 대시보드용 통계 데이터를 제공하는 서비스.
 *
 * <h2>주요 기능</h2>
 * <ul>
 *   <li>오늘 방문자 수 조회</li>
 *   <li>오늘 방문자 중 남성 비율 계산</li>
 *   <li>최근 7일간 일별 방문자·신규 가입자 수 조회</li>
 * </ul>
 *
 * <p>방문자 통계는 {@link com.lingo.lingoproject.shared.domain.model.UserAccessLog}를 기반으로 집계하며,
 * 신규 가입자 수는 {@link com.lingo.lingoproject.shared.domain.model.User}의 가입 시각을 기준으로 합니다.</p>
 */
@Service
@RequiredArgsConstructor
public class StatService {
  private final UserAccessLogRepository userAccessLogRepository;
  private final UserRepository userRepository;

  /**
   * 오늘(자정 이후) 방문자 수를 반환한다.
   *
   * @return 오늘 방문자 수
   */
  public long getTodayNumberOfVisitor(){
    LocalDateTime todayDateTime = LocalDate.now().atStartOfDay();
    return userAccessLogRepository.countByCreateAtAfter(todayDateTime);
  }

  /**
   * 오늘 방문자 중 남성 비율을 소수점 1자리(%)로 반환한다.
   *
   * <p>예: 남성 3명, 전체 10명 → {@code 30.0}</p>
   *
   * @return 남성 방문자 비율 (0.0 ~ 100.0)
   */
  public float getTodayMaleRatioOfVisitor(){
    long todayNumberOfMaleVisitor = userAccessLogRepository.countByCreateAtAndGender(LocalDateTime.now(), Gender.MALE);
    long todayNumberOfVisitor = getTodayNumberOfVisitor();
    return Math.round(((float) todayNumberOfMaleVisitor /todayNumberOfVisitor) * 100 * 10) / 10.0f;
  }

  /**
   * 최근 7일간의 일별 방문자 수와 신규 가입자 수를 반환한다.
   *
   * <p>오늘 포함 6일 전부터 오늘까지 날짜별로 집계하며,
   * 데이터가 없는 날짜는 0으로 채워 반환합니다.</p>
   *
   * @return 날짜별 방문자 수·가입자 수 목록 (7개 항목, 오래된 날짜부터 정렬)
   */
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
