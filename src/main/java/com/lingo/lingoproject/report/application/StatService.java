package com.lingo.lingoproject.report.application;

import com.lingo.lingoproject.report.presentation.dto.GetActiveUserStatResponseDto;
import com.lingo.lingoproject.report.presentation.dto.GetActiveUserStatResponseDto.GetDetailStatResponseDto;
import com.lingo.lingoproject.report.presentation.dto.GetStatOverviewResponseDto;
import com.lingo.lingoproject.report.presentation.dto.GetStatOverviewResponseDto.GenderRatio;
import com.lingo.lingoproject.report.presentation.dto.GetUserRetentionResponseDto;
import com.lingo.lingoproject.shared.domain.model.Gender;
import com.lingo.lingoproject.shared.domain.model.UserActivityLog;
import com.lingo.lingoproject.shared.infrastructure.persistence.UserAccessLogRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.UserActivityLogRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.UserRepository;
import com.lingo.lingoproject.shared.utils.RedisKey;
import com.lingo.lingoproject.user.application.UserQueryUseCase;
import com.lingo.lingoproject.report.presentation.dto.stats.GetDailyNumberOfVisitorRequestDto;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalUnit;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
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
@Slf4j
@Service
@RequiredArgsConstructor
public class StatService {
  private final UserAccessLogRepository userAccessLogRepository;
  private final UserQueryUseCase userQueryUseCase;
  private final RedisTemplate<String, Object> redisTemplate;
  private final UserActivityLogRepository userActivityLogRepository;
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
    Map<String, Long> userSignupMap = userQueryUseCase.findAllByCreatedAtAfter(startDateTime)
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

  public GetActiveUserStatResponseDto getDailyActiveUser(String date){
    LocalDate end = parseStringToDatetime(date);
    LocalDate start = end.minusDays(6L);
    Map<String, Long> 날짜_유저_맵 = userActivityLogRepository.해당_기간_사이의_유저_활동_로그_조회(start, end)
        .stream()
        .collect(Collectors.groupingBy(
            u -> u.getStart().toLocalDate().toString(),
            Collectors.collectingAndThen(
                Collectors.mapping(UserActivityLog::getUserId, Collectors.toSet()),
                set -> (long) set.size()
            )
        ));
    List<GetDetailStatResponseDto> data = 날짜_유저_맵.entrySet()
        .stream()
        .map(value -> new GetDetailStatResponseDto(value.getKey(),
            Math.toIntExact(value.getValue())))
        .toList();
    return GetActiveUserStatResponseDto.builder()
        .metric("dau")
        .data(data)
        .build();
  }

  public GetActiveUserStatResponseDto getWeeklyActiveUser(String date){
    LocalDate end = parseStringToDatetime(date);
    LocalDate start = end.minusWeeks(6L);
    Map<String, Long> 날짜_유저_맵 = userActivityLogRepository.해당_기간_사이의_유저_활동_로그_조회(start, end)
        .stream()
        .collect(Collectors.groupingBy(
            u -> u.getStart().toLocalDate().with(WeekFields.ISO.dayOfWeek(), 1).toString(),
            Collectors.collectingAndThen(
                Collectors.mapping(UserActivityLog::getUserId, Collectors.toSet()),
                set -> (long) set.size()
            )
        ));
    List<GetDetailStatResponseDto> data = 날짜_유저_맵.entrySet()
        .stream()
        .map(value -> new GetDetailStatResponseDto(value.getKey(),
            Math.toIntExact(value.getValue())))
        .toList();
    return GetActiveUserStatResponseDto.builder()
        .metric("wau")
        .data(data)
        .build();
  }

  public GetActiveUserStatResponseDto getMonthlyActiveUser(String date){
    LocalDate end = parseStringToDatetime(date);
    LocalDate start = end.minusMonths(6L);
    Map<String, Long> 날짜_유저_맵 = userActivityLogRepository.해당_기간_사이의_유저_활동_로그_조회(start, end)
        .stream()
        .collect(Collectors.groupingBy(
            u -> u.getStart().toLocalDate().withDayOfMonth(1).toString(),
            Collectors.collectingAndThen(
                Collectors.mapping(UserActivityLog::getUserId, Collectors.toSet()),
                set -> (long) set.size()
            )
        ));
    List<GetDetailStatResponseDto> data = 날짜_유저_맵.entrySet()
        .stream()
        .map(value -> new GetDetailStatResponseDto(value.getKey(),
            Math.toIntExact(value.getValue())))
        .toList();
    return GetActiveUserStatResponseDto.builder()
        .metric("mau")
        .data(data)
        .build();
  }

  public GetStatOverviewResponseDto getStatOverview(String date){
    LocalDate currentDate = parseStringToDatetime(date);
    LocalDate preDate = currentDate.minusDays(1L);

    int activeSessionCount = getActiveSessionCount();
    int activeSessionDelta = 0;

    int currentDau = 해당_날짜의_dau_값_조회(currentDate);
    int preDau = 해당_날짜의_dau_값_조회(preDate);
    int dauDelta = currentDau - preDau;

    int currentSignups = userRepository.해당_날짜에_가입한_사람_수_조회(currentDate);
    int preSignups =  userRepository.해당_날짜에_가입한_사람_수_조회(preDate);
    int signupsDelta = currentSignups - preSignups;

    Double currentAvgSessionDurationSeconds = userActivityLogRepository.해당_날짜의_세션_기간_평균_조회(currentDate);
    Double preAvgSessionDurationSeconds = userActivityLogRepository.해당_날짜의_세션_기간_평균_조회(preDate);
    currentAvgSessionDurationSeconds = currentAvgSessionDurationSeconds != null ? currentAvgSessionDurationSeconds : 0;
    preAvgSessionDurationSeconds = preAvgSessionDurationSeconds != null ? preAvgSessionDurationSeconds : 0;
    Double avgAvgSessionDurationDelta = currentAvgSessionDurationSeconds - preAvgSessionDurationSeconds;
    int avgSessionDurationSeconds = (int) Math.round(currentAvgSessionDurationSeconds);


    long activeMaleCount = userRepository.findAllByIdIn(해당_날짜의_접속_유저_id_조회(currentDate))
        .stream()
        .filter(u -> Objects.equals(Gender.MALE, u.getGender()))
        .count();
    float malePct = Math.round((activeMaleCount / (float) currentDau) * 1000) / 10.0f;
    float femalePct = activeMaleCount != 0 ? 100 - malePct : 0;

    GetStatOverviewResponseDto dto = GetStatOverviewResponseDto.builder()
        .date(date)
        .activeSessionCount(activeSessionCount)
        .activeSessionDelta(activeSessionDelta)
        .dau(currentDau)
        .dauDelta(dauDelta)
        .newSignups(currentSignups)
        .newSignupsDelta(signupsDelta)
        .avgSessionDurationSeconds(avgSessionDurationSeconds)
        .avgSessionDurationDelta((int) Math.round(avgAvgSessionDurationDelta))
        .genderRatio(new GenderRatio(malePct, femalePct))
        .build();
    return dto;
  }

  public GetUserRetentionResponseDto getUserRetention(String startDate, String endDate){
    LocalDate start = parseStringToDatetime(startDate);

    int newUserCount = 0;
    int dau = 0;

    newUserCount += userRepository.해당_날짜에_가입한_사람_수_조회(start);
    dau += 해당_날짜의_dau_값_조회(start);

    log.info("newUserCount: {}", newUserCount);
    log.info("dau: {}", dau);
    if (dau == 0) return GetUserRetentionResponseDto.builder()
        .startDate(startDate)
        .endDate(endDate)
        .retentionCount(0)
        .retentionRate(0)
        .newUserRate(0)
        .build();
    float newUserRate = (float) (Math.round(((newUserCount /(float) dau)) * 1000) / 10.0);
    float retentionRate = (float) (Math.round((100 - newUserRate) * 10) / 10.0);
    log.info("newUserRate = {}", newUserRate);
    log.info("retentionRate= {}", retentionRate);

    return GetUserRetentionResponseDto.builder()
        .startDate(startDate)
        .endDate(endDate)
        .retentionCount(dau - newUserCount)
        .retentionRate(retentionRate)
        .newUserRate(newUserRate)
        .build();
  }

  public Map<String, Object> getDailyStatOverview(String from, String to){
    LocalDate start = parseStringToDatetime(from);
    LocalDate end = parseStringToDatetime(to);
    List<GetStatOverviewResponseDto> data = new ArrayList<>();
    for (LocalDate i = start; i.isBefore(end); i = i.plusDays(1L)){
      data.add(getStatOverview(i.toString()));
    }
    Map<String, Object> response = new HashMap<>();
    response.put("from", from);
    response.put("to", to);
    response.put("data", data);
    return response;
  }

  private int 해당_날짜의_dau_값_조회(LocalDate localDate){
    return 해당_날짜의_접속_유저_id_조회(localDate).size();
  }

  private Set<Long> 해당_날짜의_접속_유저_id_조회(LocalDate localDate){
    Set<Long> userIdList =  userActivityLogRepository.해당_날짜에_접속한_유저수(localDate);
    Set<Long> activeUserIdList = getActiveSessionUserIdList();
    if (LocalDate.now().equals(localDate)){
      userIdList.addAll(activeUserIdList);
    }
    return userIdList;
  }

  private LocalDate parseStringToDatetime(String date){
    if (date == null) return LocalDate.now();
    return LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
  }

  private int getActiveSessionCount() {
    ScanOptions options = ScanOptions.scanOptions()
        .match(RedisKey.접속_유저_레디스_키 + "*")
        .count(1000)
        .build();
    try (Cursor<String> cursor = redisTemplate.scan(options)) {
      return (int) cursor.stream().distinct().count();
    }
  }

  private Set<Long> getActiveSessionUserIdList(){
    return redisTemplate.keys(RedisKey.접속_유저_레디스_키 + "*")
        .stream()
        .map(u -> u.split("::")[1])
        .mapToLong(Long::parseLong)
        .boxed()
        .collect(Collectors.toSet());
  }
}
