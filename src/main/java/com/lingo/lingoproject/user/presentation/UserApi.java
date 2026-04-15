package com.lingo.lingoproject.user.presentation;

import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.user.presentation.dto.LoginInfoDto;
import com.lingo.lingoproject.user.presentation.dto.SignupResponseDto;
import com.lingo.lingoproject.user.presentation.dto.SignupUserInfoDto;
import com.lingo.lingoproject.shared.security.dto.LoginResponseDto;
import com.lingo.lingoproject.shared.security.dto.RegenerateTokenResponseDto;
import com.lingo.lingoproject.user.presentation.dto.GetFriendInvitationCodeResponseDto;
import com.lingo.lingoproject.user.presentation.dto.GetUserInfoResponseDto;
import com.lingo.lingoproject.user.presentation.dto.GetUserLoginIdResponseDto;
import com.lingo.lingoproject.user.presentation.dto.ResetPasswordRequestDto;
import com.lingo.lingoproject.user.presentation.dto.SaveMembershipRequestDto;
import com.lingo.lingoproject.user.presentation.dto.UpdateUserInfoRequestDto;
import com.lingo.lingoproject.shared.utils.ResultMessageResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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

  @Operation(summary = "로그인", description = "커스텀 인증 필터에서 처리한 로그인 정보를 사용해 토큰을 발급합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "0000", description = "로그인 성공", content = @Content(schema = @Schema(implementation = LoginResponseDto.class))),
      @ApiResponse(responseCode = "E0003", description = "유저가 인증되지 않았습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E0005", description = "유저 객체를 찾을 수 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
  })
  @PostMapping("/login")
  ResponseEntity<LoginResponseDto> login(
      /*
       * swagger용 requestBody입니다.
       */
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "login 요청 dto, filter 단에서 값을 먼저 읽고 처리함",
          content =  @Content(schema = @Schema(implementation = LoginInfoDto.class))
      )LoginInfoDto loginInfoDto,
      HttpServletRequest request,
      @AuthenticationPrincipal User user
  );

  @Operation(summary = "토큰 재발급", description = "리프레시 토큰을 사용해 새로운 액세스/리프레시 토큰을 발급합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "0000", description = "토큰 재발급 성공", content = @Content(schema = @Schema(implementation = LoginResponseDto.class))),
      @ApiResponse(responseCode = "E0005", description = "유저 객체를 찾을 수 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
  })
  @GetMapping("/refresh")
  ResponseEntity<RegenerateTokenResponseDto> refresh(
      @Parameter(description = "리프레시 토큰 값", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...") @RequestParam String refreshToken
  );

  @Operation(summary = "회원 가입", description = "회원 가입에 필요한 정보로 계정을 생성합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "0000", description = "회원가입 성공", content = @Content(schema = @Schema(implementation = LoginResponseDto.class))),
      @ApiResponse(responseCode = "E0007", description = "아이디 또는 비밀번호의 형식이 불일치합니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E0008", description = "중복된 아이디입니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
  })
  @PostMapping("/signup")
  ResponseEntity<SignupResponseDto> signup(@Valid @RequestBody LoginInfoDto dto);

  @Operation(summary = "회원 정보 입력", description = "회원가입 시 유저 정보 저장")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "0000", description = "회원정보 저장 성공", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E0007", description = "잘못된 파라미터가 전달되었습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E0005", description = "유저 객체를 찾을 수 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E0015", description = "미성년자는 가입할 수 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E0003", description = "본인인증되지 않은 회원입니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E0014", description = "블락된 유저입니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
  })
  @PostMapping("/signup/user-info")
  ResponseEntity<ResultMessageResponseDto> signupUserInfo(@Valid @RequestBody SignupUserInfoDto dto);

  @Operation(summary = "아이디 중복 확인", description = "중복시 NOT_ACCEPTABLE(406) 반환 ")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "0000", description = "사용가능한 아이디", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E0008", description = "중복된 아이디입니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
  })
  @GetMapping("/signup/check-loginId")
  ResponseEntity<ResultMessageResponseDto> verifyDuplicatedLoginId(@RequestParam(value = "loginId") String loginId);

  @Operation(summary = "닉네임 중복확인")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "0000", description = "사용가능한 닉네임", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E0008", description = "중복된 닉네임입니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
  })
  @GetMapping("/signup/check-nickname")
  ResponseEntity<ResultMessageResponseDto> verifyDuplicatedNickname(@RequestParam(value = "nickname") String nickname);


  @Operation(summary = "로그아웃", description = "헤더의 액세스 토큰을 무효화합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "0000", description = "로그아웃 성공", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
  })
  @GetMapping("/api/logout")
  ResponseEntity<ResultMessageResponseDto> logout(HttpServletRequest request);

  @Operation(summary = "유저 회원 탈퇴", description = "탈퇴 원인 저장과 유저 정보 삭제")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "0000", description = "회원 탈퇴 성공", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E0003", description = "유저를 탈퇴할 권한이 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
      }
  )
  @DeleteMapping("/users/{user-id}")
  ResponseEntity<ResultMessageResponseDto> deleteUser(
      @Parameter(description = "유저id", example = "4")
      @PathVariable(value = "user-id") Long userId,

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
  @GetMapping("/users/{user-id}")
  ResponseEntity<GetUserInfoResponseDto> getUserInfo(@PathVariable(value = "user-id") Long userId, @AuthenticationPrincipal User user);

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
