package com.lingo.lingoproject.report.presentation;
import com.lingo.lingoproject.report.application.StatService;
import com.lingo.lingoproject.report.presentation.dto.GetActiveUserStatResponseDto;
import com.lingo.lingoproject.report.presentation.dto.GetStatOverviewResponseDto;
import com.lingo.lingoproject.report.presentation.dto.GetUserRetentionResponseDto;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.report.presentation.dto.stats.GetDailyNumberOfVisitorRequestDto;
import com.lingo.lingoproject.report.presentation.dto.stats.GetTodayVisitorStatsRequestDto;
import com.lingo.lingoproject.shared.utils.ApiListResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping()
@Tag(name = "stats-controller", description = "유저 통계 관련 api")
public class StatsController {

  private final StatService statService;

  @Operation(summary = "일일 방문객 수 조회")
  @GetMapping
  public ResponseEntity<GetTodayVisitorStatsRequestDto> getTodayNumberOfVisitor(){
    log.info("step=일일_방문자수_조회_시작");
    long countOfTodayVisitors = statService.getTodayNumberOfVisitor();
    float ratioOfTodayMaleVisitor = statService.getTodayMaleRatioOfVisitor();
    float ratioOfTodayFemaleVisitor = 100 - ratioOfTodayMaleVisitor;
    log.info("step=일일_방문자수_조회_완료, count={}, maleRatio={}, femaleRatio={}", countOfTodayVisitors, ratioOfTodayMaleVisitor, ratioOfTodayFemaleVisitor);

    return ResponseEntity.status(HttpStatus.OK).body(new GetTodayVisitorStatsRequestDto(countOfTodayVisitors, ratioOfTodayMaleVisitor, ratioOfTodayFemaleVisitor));
  }

  @Operation(summary = "일주일 간 일일 방문객 수 조회")
  @GetMapping("/daily")
  public ResponseEntity<ApiListResponseDto<GetDailyNumberOfVisitorRequestDto>> getDailyNumberOfVisitor(){
    log.info("step=주간_방문자수_조회_시작");
    List<GetDailyNumberOfVisitorRequestDto> visitors = statService.getDailyNumberOfVisitorForWeek();
    log.info("step=주간_방문자수_조회_완료, count={}", visitors.size());
    return ResponseEntity.status(HttpStatus.OK).body(new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), visitors));
  }

  @GetMapping("/api/analytics/overview")
  public ResponseEntity<GetStatOverviewResponseDto> getStatOverview(@RequestParam(value = "date", required = false) String date){
    GetStatOverviewResponseDto response = statService.getStatOverview(date);
    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  @GetMapping("/api/analytics/active-users")
  public ResponseEntity<GetActiveUserStatResponseDto> getActiveUser(
      @RequestParam("metric") String metric,
      @RequestParam("from") String from,
      @RequestParam("to") String to
  ){
    GetActiveUserStatResponseDto response = switch (metric.toUpperCase()){
      case "DAU" -> statService.getDailyActiveUser(to);
      case "WAU" -> statService.getWeeklyActiveUser(to);
      case "MAU" -> statService.getMonthlyActiveUser(to);
      default -> null;
    };
    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  @GetMapping("/api/analytics/daily-summary")
  public ResponseEntity<Map<String, Object>> getDailyStatOverview(
      @RequestParam("from") String from,
      @RequestParam("to") String to
  ){
    Map<String, Object> response = statService.getDailyStatOverview(from, to);
    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  @GetMapping("/api/analytics/retention")
  public ResponseEntity<GetUserRetentionResponseDto> getUserRetention(
      @RequestParam("startDate") String startDate,
      @RequestParam("endDate") String endDate
  ){
    log.info("retention count 시작");
    GetUserRetentionResponseDto response = statService.getUserRetention(startDate, endDate);
    return ResponseEntity.status(HttpStatus.OK).body(response);
  }
}
