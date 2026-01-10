package com.lingo.lingoproject.report;

import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.exception.ErrorCode;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.report.dto.SaveReportRequestDto;
import com.lingo.lingoproject.utils.ResultMessageResponseDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
@Tag(name = "report-controller", description = "유저 신고 관련 api")
public class ReportController {

  private final ReportService reportService;

  @PostMapping("/reports")
  public ResponseEntity<ResultMessageResponseDto> report(
      @Valid @RequestBody SaveReportRequestDto dto,
      @AuthenticationPrincipal User user
  ){
    if (!dto.reportUserId().equals(user.getId())){
      log.error("userId={}, reportUserId={}, step=신고_요청, status=FAILED, reason=권한없음", user.getId(), dto.reportUserId());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResultMessageResponseDto(
          ErrorCode.NO_AUTH.getCode(), "신고할 권한이 없습니다."));
    }
    log.info("userId={}, reportedUserId={}, step=신고_요청_시작, status=SUCCESS", user.getId(), dto.reportedUserId());
    try {
      reportService.report(dto);
      log.info("userId={}, reportedUserId={}, step=신고_요청_완료, status=SUCCESS", user.getId(), dto.reportedUserId());
      return ResponseEntity.status(HttpStatus.OK)
          .body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "신고가 성공적으로 접수되었습니다."));
    } catch (Exception e) {
      log.error("userId={}, reportedUserId={}, step=신고_요청_실패, status=FAILED", user.getId(), dto.reportedUserId(), e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("신고 접수에 실패했습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

}
