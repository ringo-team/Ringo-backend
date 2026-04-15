package com.lingo.lingoproject.report.presentation;
import com.lingo.lingoproject.report.application.ReportService;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.report.presentation.dto.SaveReportRequestDto;
import com.lingo.lingoproject.shared.utils.ResultMessageResponseDto;
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
      log.error("step=신고_요청_권한없음, userId={}, reportUserId={}", user.getId(), dto.reportUserId());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResultMessageResponseDto(
          ErrorCode.NO_AUTH.getCode(), "신고할 권한이 없습니다."));
    }
    log.info("step=신고_요청_시작, userId={}, reportedUserId={}", user.getId(), dto.reportedUserId());
    reportService.report(dto);
    log.info("step=신고_요청_완료, userId={}, reportedUserId={}", user.getId(), dto.reportedUserId());

    return ResponseEntity.status(HttpStatus.OK)
        .body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "신고가 성공적으로 접수되었습니다."));
  }

}
