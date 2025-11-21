package com.lingo.lingoproject.user;

import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.user.dto.GetFriendInvitationCodeRequestDto;
import com.lingo.lingoproject.user.dto.GetUserInfoResponseDto;
import com.lingo.lingoproject.user.dto.GetUserLoginIdResponseDto;
import com.lingo.lingoproject.user.dto.ResetPasswordRequestDto;
import com.lingo.lingoproject.user.dto.UpdateUserInfoRequestDto;
import com.lingo.lingoproject.utils.ResultMessageResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @Operation(
      summary = "유저 회원 탈퇴",
      description = "탈퇴 원인 저장과 유저 정보 삭제"
  )
  @DeleteMapping("/{id}")
  public ResponseEntity<ResultMessageResponseDto> deleteUser(
      @Parameter(description = "유저id", example = "4")
      @PathVariable Long id,

      @Parameter(description = "유저 탈퇴 사유", example = "좋은 인연을 만날 수 없어서")
      @NotBlank
      @RequestParam(value = "reason") String reason,

      @AuthenticationPrincipal User user){
    Long userId = user.getId();
    if(!userId.equals(id)){
      throw new RingoException("쟐못된 접근입니다.", HttpStatus.FORBIDDEN);
    }
    userService.deleteUser(id, reason);
    return ResponseEntity.ok().body(new ResultMessageResponseDto("유저를 성공적으로 삭제하였습니다."));
  }

  @Operation(
      summary = "유저 id 찾기",
      description = "본인인증 성공한 유저의 id 찾기"
  )
  @GetMapping("/find-id")
  public ResponseEntity<GetUserLoginIdResponseDto> findUserId(@AuthenticationPrincipal User user){
    String username = userService.findUserId(user.getId());
    return ResponseEntity.ok().body(new GetUserLoginIdResponseDto(username));
  }

  @Operation(
      summary = "유저 password 재설정",
      description = "본인인증 성공한 유저 password 재설정"
  )
  @PatchMapping("reset-password")
  public ResponseEntity<ResultMessageResponseDto> resetPassword(@RequestBody ResetPasswordRequestDto dto, @AuthenticationPrincipal User user){
    userService.resetPassword(dto.password(), user.getId());
    return ResponseEntity.ok().body(new ResultMessageResponseDto("password를 성공적으로 변경하였습니다."));
  }

  @Operation(
      summary = "유저 정보 조회",
      description = "프로필에 존재하는 유저 정보를 조회하는 api"
  )
  @GetMapping()
  public ResponseEntity<GetUserInfoResponseDto> getUserInfo(@AuthenticationPrincipal User user){
    GetUserInfoResponseDto dto = userService.getUserInfo(user.getId());
    return ResponseEntity.ok().body(dto);
  }

  @Operation(
      summary = "유저 정보 업데이트",
      description = "수정할 수 있는 유저 정보를 업데이트 하는 api"
  )
  @PatchMapping()
  public ResponseEntity<ResultMessageResponseDto> updateUserInfo(@RequestBody UpdateUserInfoRequestDto dto, @AuthenticationPrincipal User user){
    userService.updateUserInfo(user.getId(), dto);
    return ResponseEntity.ok().body(new ResultMessageResponseDto("정상적으로 수정되었습니다."));
  }

  @Operation(summary = "친구초대코드 조회")
  @GetMapping("invitation-code")
  public ResponseEntity<GetFriendInvitationCodeRequestDto> getInvitationCode(@AuthenticationPrincipal User user){
    return ResponseEntity.ok().body(new GetFriendInvitationCodeRequestDto(user.getFriendInvitationCode()));
  }

  @Operation(summary = "친구초대코드 입력", description = "친구초대코드 입력 및 보상 받기")
  @PostMapping("invitation-code")
  public ResponseEntity<ResultMessageResponseDto> inputInvitationCodeAndGetReward(@AuthenticationPrincipal User user, @RequestParam String code){
    userService.checkFriendInvitationCodeAndProvideReward(user, code);
    return ResponseEntity.ok().body(new ResultMessageResponseDto("친구와 본인 모두 보상을 획득하였습니다."));
  }

  @Operation(summary = "휴면 계정을 업데이트합니다.", description = "계정을 휴면시키거나 해제시킵니다.")
  @PostMapping("dormant")
  public ResponseEntity<ResultMessageResponseDto> updateDormantAccount(
      @Parameter(description = "계정 휴면 혹은 휴면 해제",
        schema = @Schema(example = "DORMANT", allowableValues = {"DORMANT", "CANCEL"})
      )@RequestParam(value = "request") String request,

      @AuthenticationPrincipal User user){

    userService.updateDormantAccount(user, request);
    return ResponseEntity.ok().body(new ResultMessageResponseDto("휴면 계정 정보를 업데이트 하였습니다."));
  }

}
