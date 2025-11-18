package com.lingo.lingoproject.stats;

import com.lingo.lingoproject.stats.dto.GetDailyNumberOfVisitorRequestDto;
import com.lingo.lingoproject.stats.dto.GetTodayVisitorStatsRequestDto;
import com.lingo.lingoproject.utils.JsonListWrapper;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/stats")
public class StatsController {

  private final StatService statService;

  @Operation(summary = "일일 방문객 수 조회")
  @GetMapping
  public ResponseEntity<GetTodayVisitorStatsRequestDto> getTodayNumberOfVisitor(){
    long countOfTodayVisitors = statService.getTodayNumberOfVisitor();
    float ratioOfTodayMaleVisitor = statService.getTodayMaleRatioOfVisitor();
    float ratioOfTodayFemaleVisitor = 100 - ratioOfTodayMaleVisitor;

    return ResponseEntity.status(HttpStatus.OK).body(new GetTodayVisitorStatsRequestDto(countOfTodayVisitors, ratioOfTodayMaleVisitor, ratioOfTodayFemaleVisitor));
  }

  @Operation(summary = "일주일 간 일일 방문객 수 조회")
  @GetMapping("/daily")
  public ResponseEntity<JsonListWrapper<GetDailyNumberOfVisitorRequestDto>> getDailyNumberOfVisitor(){
    return ResponseEntity.status(HttpStatus.OK).body(new JsonListWrapper<>(statService.getDailyNumberOfVisitor()));
  }

  

}
