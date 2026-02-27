package com.lingo.lingoproject.user;

import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.user.dto.GetFriendInvitationCodeResponseDto;
import com.lingo.lingoproject.user.dto.GetUserInfoResponseDto;
import com.lingo.lingoproject.user.dto.GetUserLoginIdResponseDto;
import com.lingo.lingoproject.user.dto.ResetPasswordRequestDto;
import com.lingo.lingoproject.user.dto.SaveMembershipRequestDto;
import com.lingo.lingoproject.user.dto.UpdateUserInfoRequestDto;
import com.lingo.lingoproject.utils.ResultMessageResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "user-controller", description = "유저 휴면/탈퇴/조회/친구초대 관련 api")
public interface UserApi {

  @Operation(summary = "유저 회원 탈퇴", description = "탈퇴 원인 저장과 유저 정보 삭제")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "0000", description = "회원 탈퇴 성공", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E0003", description = "유저를 탈퇴할 권한이 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
      }
  )
  @DeleteMapping("/users/{userId}")
  ResponseEntity<ResultMessageResponseDto> deleteUser(
      @Parameter(description = "유저id", example = "4") @PathVariable Long userId,

      @Parameter(description = "유저 탈퇴 사유", example = "좋은 인연을 만날 수 없어서")
      @NotBlank @Length(max = 100) @RequestParam(value = "reason") String reason,

      @Parameter(description = "피드백", example = "앱 피드백")
      @Length(max = 1000) @RequestParam(value = "feedback") String feedback,

      @AuthenticationPrincipal User user
  );

  @Operation(summary = "유저 id 찾기", description = "본인인증 성공한 유저의 id 찾기")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "0000", description = "조회 성공", content = @Content(schema = @Schema(implementation = GetUserLoginIdResponseDto.class))),
          @ApiResponse(responseCode = "E0003", description = "아이디를 조회할 권한이 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
  })
  @GetMapping("/users/find-id")
  ResponseEntity<GetUserLoginIdResponseDto> findUserLoginId(@AuthenticationPrincipal User user);

  @Operation(summary = "유저 password 재설정", description = "본인인증 성공한 유저 password 재설정")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "0000", description = "재설정 성공", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E0003", description = "비밀번호를 재설정할 권한이 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
  })
  @PatchMapping("/users/reset-password")
  ResponseEntity<ResultMessageResponseDto> resetPassword(@RequestBody ResetPasswordRequestDto dto, @AuthenticationPrincipal User user);

  @Operation(summary = "유저 정보 조회", description = "프로필에 존재하는 유저 정보를 조회하는 api")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "0000", description = "조회 성공", content = @Content(schema = @Schema(implementation = GetUserInfoResponseDto.class))),
          @ApiResponse(responseCode = "E0003", description = "유저 정보를 조회할 권한이 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E0005", description = "해당 userId는 가입된 유저의 아이디가 아닙니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
  })
  @GetMapping("/users/{userId}")
  ResponseEntity<GetUserInfoResponseDto> getUserInfo(@PathVariable(value = "userId") Long userId, @AuthenticationPrincipal User user);

  @Operation(summary = "유저 정보 업데이트", description = "수정할 수 있는 유저 정보를 업데이트 하는 api")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "0000", description = "업데이트 성공", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
  })
  @PatchMapping("/users")
  ResponseEntity<ResultMessageResponseDto> updateUserInfo(@RequestBody UpdateUserInfoRequestDto dto, @AuthenticationPrincipal User user);

  @Operation(summary = "친구초대코드 조회")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "0000", description = "조회 성공", content = @Content(schema = @Schema(implementation = GetFriendInvitationCodeResponseDto.class))),
          @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
  })
  @GetMapping("/users/invitation-code")
  ResponseEntity<GetFriendInvitationCodeResponseDto> getInvitationCode(@AuthenticationPrincipal User user);

  @Operation(summary = "친구초대코드 입력", description = "친구초대코드 입력 및 보상 받기")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "0000", description = "입력 성공", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E0008", description = "초대 코드 입력횟수를 초과하였습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E0001", description = "잘못된 코드를 입력하였습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
  })
  @PostMapping("/users/invitation-code")
  ResponseEntity<ResultMessageResponseDto> inputInvitationCodeAndGetReward(@AuthenticationPrincipal User user, @RequestParam String code);


  @Operation(summary = "휴면 계정을 업데이트합니다.", description = "계정을 휴면시키거나 해제시킵니다. 유저가 휴면 상태에 들어가면 이성추천에서 유저가 배제됩니다.")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "0000", description = "업데이트 성공", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
  })
  @PostMapping("/users/dormant")
  ResponseEntity<ResultMessageResponseDto> updateDormantAccount(@AuthenticationPrincipal User user);

  @Operation(summary = "유저의 접근정보를 저장합니다.", description = "유저가 앱을 실행할 때 이 api를 호출합니다.")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "0000", description = "저장 성공", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
  })
  @PostMapping("/users/access")
  ResponseEntity<ResultMessageResponseDto> saveUserAccessLog(@AuthenticationPrincipal User user);

  @Operation(summary = "멤버십을 구독합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "0000", description = "성공", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
  })
  @PostMapping("/memberships")
  ResponseEntity<ResultMessageResponseDto> saveMembership(@RequestBody SaveMembershipRequestDto dto, @AuthenticationPrincipal User user);
}
