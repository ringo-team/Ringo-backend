package com.lingo.lingoproject.report;

import com.lingo.lingoproject.report.dto.SaveReportRequestDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

  private final ReportService reportService;

  @PostMapping()
  public ResponseEntity<String> report(
      @Valid @RequestBody SaveReportRequestDto dto
  ){
    reportService.report(dto);
    return ResponseEntity.ok().body("신고가 성공적으로 접수되었습니다.");
  }
}
