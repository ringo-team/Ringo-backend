package com.lingo.lingoproject.admin;


import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.exception.ErrorCode;
import com.lingo.lingoproject.report.ReportService;
import com.lingo.lingoproject.report.dto.GetReportInfoRequestDto;
import com.lingo.lingoproject.report.dto.GetReportInfoResponseDto;
import com.lingo.lingoproject.user.UserService;
import com.lingo.lingoproject.user.dto.GetUserInfoResponseDto;
import com.lingo.lingoproject.utils.JsonListWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.lingo.lingoproject.exception.RingoException;

@RestController
@RequestMapping("/admin")
@Slf4j
@RequiredArgsConstructor
public class AdminController {

  private final UserService userService;
  private final ReportService reportService;

  @Operation(
      summary = "유저들의 정보 조회",
      description = "관리자 페이지의 유저 정보들을 조회하는 페이짖"
  )
  @GetMapping("/users")
  public ResponseEntity<?> getUsersInfo(
      @Parameter(description = "페이지 수", example = "2")
      @RequestParam int page,

      @Parameter(description = "페이지 크기", example = "5")
      @RequestParam int size){
    try {
      log.info("page={}, size={}, step=관리자_유저정보_조회_시작, status=SUCCESS", page, size);
      List<GetUserInfoResponseDto> dtos = userService.getPageableUserInfo(page, size);
      log.info("page={}, size={}, count={}, step=관리자_유저정보_조회_완료, status=SUCCESS", page, size, dtos.size());
      return ResponseEntity.status(HttpStatus.OK).body(dtos);
    } catch (Exception e) {
      log.error("page={}, size={}, step=관리자_유저정보_조회_실패, status=FAILED", page, size, e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("관리자 유저 정보 조회에 실패했습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

//  @Operation(
//      summary = "유저 블락 기능"
//  )
//  @PostMapping("users/{id}/blocks")
//  public ResponseEntity<?> blockUser(
//      @Parameter(description = "블락시키고자 하는 유저 id", example = "5")
//      @PathVariable Long id,
//
//      @AuthenticationPrincipal User admin){
//    userService.blockUser(id, admin.getId());
//    return ResponseEntity.ok().build();
//  }

  /* 관리자더라도 유저삭제를 불가능하게 함

  @Operation(
      summary = "유저 영구 삭제",
      description = "유저와 관련된 모든 정보를 없애거나 관계를 끊어버린다."
  )
  @DeleteMapping("/{id}")
  public ResponseEntity<?> deleteUser(
      @Parameter(description = "삭제하고자 하는 유저id")
      @PathVariable Long id){
    userService.deleteUser(id, "관리자 삭제");
    return ResponseEntity.ok().build();
  }
*/

  @Operation(summary = "신고조회", description = "조건에 따라 필터링된 신고된 유저 조회")
  @GetMapping("/reports")
  public ResponseEntity<JsonListWrapper<GetReportInfoResponseDto>> getReportInfos(
      @Parameter(description = "신고자 혹은 피신고자의 id", example = "5")
      @RequestParam(required = false)
      Long userId,

      @Parameter(description = "신고자 상태",
          schema = @Schema(
              example = "PENDING",
              allowableValues = {
                  "PENDING", "PERMANENT_ACCOUNT_SUSPENSION", "SEVERE_ACCOUNT_SUSPENSION",
                  "RISKY_ACCOUNT_SUSPENSION", "MINOR_ACCOUNT_SUSPENSION", "WARNING",
                  "LEGAL_REVIEW", "INNOCENT_REPORT"
              })
      )
      @RequestParam(required = false)
      String reportedUserStatus,

      @Parameter(description = "신고 강도",
          schema = @Schema(
              example = "PENDING",
              allowableValues = {"PENDING", "MINOR", "WARNING", "SEVERE", "ILLEGAL"})
      )
      @RequestParam(required = false)
      String reportIntensity,

      @Parameter(description = "정렬",
          schema = @Schema(
              example = "CREATED_AT_DESC",
              allowableValues = {"CREATED_AT_DESC", "CREATED_AT_ASC", "STATUS_DESC",
                  "STATUS_ASC", "INTENSITY_DESC", "INTENSITY_ASC"})
      )
      @RequestParam(required = false)
      String ordering,

      @Parameter(description = "신고 조회 시작일", example = "2025-08-27")
      @RequestParam(required = false)
      String startedAt,

      @Parameter(description = "신고 조회 마감일", example = "2025-09-02")
      @RequestParam(required = false)
      String finishedAt
  ){
    try {
      log.info("step=신고정보_조회_시작, status=SUCCESS, userId={}, reportedUserStatus={}, reportIntensity={}, ordering={}, startedAt={}, finishedAt={}", userId, reportedUserStatus, reportIntensity, ordering, startedAt, finishedAt);
      List<GetReportInfoResponseDto> list = reportService.getReportInfos(new GetReportInfoRequestDto(
          userId, reportedUserStatus, reportIntensity, ordering, startedAt, finishedAt
      ));
      log.info("count={}, step=신고정보_조회_완료, status=SUCCESS", list.size());
      return ResponseEntity.status(HttpStatus.OK).body(new JsonListWrapper<>(ErrorCode.SUCCESS.getCode(), list));
    } catch (Exception e) {
      log.error("step=신고정보_조회_실패, status=FAILED", e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("신고 정보를 조회하는데 실패했습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }


  @Operation(summary = "신고된 유저 상태 변경", description = "신고된 유저 상태변경 및 조치")
  @PatchMapping("/reports")
  public ResponseEntity<String> updateReportInfo(
      @Parameter(description = "신고 조치 상태",
      schema = @Schema(
          example = "PENDING",
          allowableValues = {
              "PENDING", "INNOCENT_REPORT", "WARNING",
              "MINOR_ACCOUNT_SUSPENSION", "RISKY_ACCOUNT_SUSPENSION", "SEVERE_ACCOUNT_SUSPENSION",
              "PERMANENT_ACCOUNT_SUSPENSION", "LEGAL_REVIEW"
          }
      ))
      @RequestParam String reportedUserStatus,

      @Parameter(description = "신고 id", example = "24")
      @RequestParam Long reportId,

      @AuthenticationPrincipal User admin
  ){
    try {

      log.info("adminId={}, reportId={}, step=신고조치_시작, status=SUCCESS", admin.getId(), reportId);
      reportService.suspendUser(reportId, reportedUserStatus, admin.getId());
      log.info("adminId={}, reportId={}, step=신고조치_완료, status=SUCCESS", admin.getId(), reportId);

      return ResponseEntity.status(HttpStatus.OK).body("성공적으로 신고조치가 완료되었습니다.");
    } catch (Exception e) {
      log.error("adminId={}, reportId={}, step=신고조치_실패, status=FAILED", admin.getId(), reportId, e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("신고 조치에 실패했습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

}
