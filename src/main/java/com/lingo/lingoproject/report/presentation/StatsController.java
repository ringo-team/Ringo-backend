package com.lingo.lingoproject.report.presentation;
import com.lingo.lingoproject.report.application.StatService;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.report.presentation.dto.stats.GetDailyNumberOfVisitorRequestDto;
import com.lingo.lingoproject.report.presentation.dto.stats.GetTodayVisitorStatsRequestDto;
import com.lingo.lingoproject.shared.utils.ApiListResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/admin/stats")
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

}
