package com.lingo.lingoproject.report;

import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.report.dto.SaveReportRequestDto;
import com.lingo.lingoproject.utils.ResultMessageResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ReportController {

  private final ReportService reportService;

  @PostMapping("/reports")
  public ResponseEntity<ResultMessageResponseDto> report(
      @Valid @RequestBody SaveReportRequestDto dto,
      @AuthenticationPrincipal User user
  ){
    if (!dto.reportUserId().equals(user.getId())){
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResultMessageResponseDto("신고할 권한이 없습니다."));
    }
    reportService.report(dto);
    return ResponseEntity.ok().body(new ResultMessageResponseDto("신고가 성공적으로 접수되었습니다."));
  }

}
