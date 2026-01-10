package com.lingo.lingoproject.stats;

import com.lingo.lingoproject.exception.ErrorCode;
import com.lingo.lingoproject.stats.dto.GetDailyNumberOfVisitorRequestDto;
import com.lingo.lingoproject.stats.dto.GetTodayVisitorStatsRequestDto;
import com.lingo.lingoproject.utils.JsonListWrapper;
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
import com.lingo.lingoproject.exception.RingoException;

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
    try {
      log.info("step=일일_방문자수_조회_시작, status=SUCCESS");
      long countOfTodayVisitors = statService.getTodayNumberOfVisitor();
      float ratioOfTodayMaleVisitor = statService.getTodayMaleRatioOfVisitor();
      float ratioOfTodayFemaleVisitor = 100 - ratioOfTodayMaleVisitor;
      log.info("step=일일_방문자수_조회_완료, status=SUCCESS, count={}, maleRatio={}, femaleRatio={}", countOfTodayVisitors, ratioOfTodayMaleVisitor, ratioOfTodayFemaleVisitor);

      return ResponseEntity.status(HttpStatus.OK).body(new GetTodayVisitorStatsRequestDto(countOfTodayVisitors, ratioOfTodayMaleVisitor, ratioOfTodayFemaleVisitor));
    } catch (Exception e) {
      log.error("step=일일_방문자수_조회_실패, status=FAILED", e);
      if (e instanceof RingoException re) {
        throw re;
      }
      throw new RingoException("일일 방문자 수 조회에 실패했습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(summary = "일주일 간 일일 방문객 수 조회")
  @GetMapping("/daily")
  public ResponseEntity<JsonListWrapper<GetDailyNumberOfVisitorRequestDto>> getDailyNumberOfVisitor(){
    try {
      log.info("step=주간_방문자수_조회_시작, status=SUCCESS");
      List<GetDailyNumberOfVisitorRequestDto> visitors = statService.getDailyNumberOfVisitorForWeek();
      log.info("count={}, step=주간_방문자수_조회_완료, status=SUCCESS", visitors.size());
      return ResponseEntity.status(HttpStatus.OK)
          .body(new JsonListWrapper<>(ErrorCode.SUCCESS.getCode(), visitors));
    } catch (Exception e) {
      log.error("step=주간_방문자수_조회_실패, status=FAILED", e);
      if (e instanceof RingoException re) {
        throw re;
      }
      throw new RingoException("주간 방문자 수 조회에 실패했습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

}
