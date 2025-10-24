package com.lingo.lingoproject.user;


import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.report.ReportService;
import com.lingo.lingoproject.report.dto.GetReportInfoRequestDto;
import com.lingo.lingoproject.report.dto.GetReportInfoResponseDto;
import com.lingo.lingoproject.user.dto.GetUserInfoResponseDto;
import com.lingo.lingoproject.utils.JsonListWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class UserManagementController {

  private final UserService userService;
  private final ReportService reportService;

  @Operation(
      summary = "유저들의 정보 조회",
      description = "관리자 페이지의 유저 정보들을 조회하는 페이짖"
  )
  @GetMapping()
  public ResponseEntity<?> getUsersInfo(
      @Parameter(description = "페이지 수", example = "2")
      @RequestParam int page,

      @Parameter(description = "페이지 크기", example = "5")
      @RequestParam int size){
    List<GetUserInfoResponseDto> dtos = userService.getPageableUserInfo(page, size);
    return ResponseEntity.ok().body(dtos);
  }

  @Operation(
      summary = "유저 블락 기능"
  )
  @PostMapping("users/{id}/blocks")
  public ResponseEntity<?> blockUser(
      @Parameter(description = "블락시키고자 하는 유저 id", example = "5")
      @PathVariable Long id,

      @AuthenticationPrincipal User admin){
    userService.blockUser(id, admin.getId());
    return ResponseEntity.ok().build();
  }

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
              allowableValues = {"PENDING", "PERMANENT_ACCOUNT_SUSPENSION", "SEVERE_ACCOUNT_SUSPENSION",
                  "RISKY_ACCOUNT_SUSPENSION", "MINOR_ACCOUNT_SUSPENSION", "WARNING", "LEGAL_REVIEW", "INNOCENT_REPORT"})
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
    List<GetReportInfoResponseDto> list = reportService.getReportInfos(new GetReportInfoRequestDto(
        userId, reportedUserStatus, reportIntensity, ordering, startedAt, finishedAt
    ));
    return ResponseEntity.ok().body(new JsonListWrapper<>(list));
  }

  @Operation(summary = "신고된 유저 상태 변경", description = "신고된 유저 상태변경 및 조치")
  @PatchMapping("/reports")
  public ResponseEntity<String> updateReportInfo(
      @Parameter(description = "신고 조치 상태",
      schema = @Schema(
          example = "PENDING",
          allowableValues = {"PENDING", "INNOCENT_REPORT", "WARNING",
              "MINOR_ACCOUNT_SUSPENSION", "RISKY_ACCOUNT_SUSPENSION", "SEVERE_ACCOUNT_SUSPENSION",
              "PERMANENT_ACCOUNT_SUSPENSION", "LEGAL_REVIEW"}
      ))
      @RequestParam String reportedUserStatus,

      @Parameter(description = "신고 id", example = "24")
      @RequestParam Long reportId,

      @AuthenticationPrincipal User admin
  ){
    reportService.suspendUser(reportId, reportedUserStatus, admin.getId());

    return ResponseEntity.ok().body("성공적으로 신고조치가 완료되었습니다.");
  }

}
