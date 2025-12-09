package com.lingo.lingoproject.user;

import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.domain.UserAccessLog;
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
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
      throw new RingoException("유저를 탈퇴할 권한이 없습니다.", HttpStatus.FORBIDDEN);
    }
    log.info("userId={}, step=회원탈퇴_시작, status=SUCCESS", userId);
    try {
      userService.deleteUser(id, reason);
      log.info("userId={}, step=회원탈퇴_완료, status=SUCCESS", userId);
      return ResponseEntity.ok().body(new ResultMessageResponseDto("유저를 성공적으로 삭제하였습니다."));
    } catch (Exception e) {
      log.error("userId={}, step=회원탈퇴_실패, status=FAILED", userId, e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("회원 탈퇴에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(
      summary = "유저 id 찾기",
      description = "본인인증 성공한 유저의 id 찾기"
  )
  @GetMapping("/find-id")
  public ResponseEntity<GetUserLoginIdResponseDto> findUserEmail(@AuthenticationPrincipal User user){
    log.info("userId={}, step=유저_ID찾기_시작, status=SUCCESS", user.getId());
    try {
      String username = userService.findUserEmail(user.getId());
      log.info("userId={}, step=유저_ID찾기_완료, status=SUCCESS", user.getId());
      return ResponseEntity.ok().body(new GetUserLoginIdResponseDto(username));
    } catch (Exception e) {
      log.error("userId={}, step=유저_ID찾기_실패, status=FAILED", user.getId(), e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("유저 아이디 조회에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(
      summary = "유저 password 재설정",
      description = "본인인증 성공한 유저 password 재설정"
  )
  @PatchMapping("reset-password")
  public ResponseEntity<ResultMessageResponseDto> resetPassword(@RequestBody ResetPasswordRequestDto dto, @AuthenticationPrincipal User user){
    try {
      log.info("userId={}, step=비밀번호_재설정_시작, status=SUCCESS", user.getId());
      userService.resetPassword(dto.password(), user.getId());
      log.info("userId={}, step=비밀번호_재설정_완료, status=SUCCESS", user.getId());

      return ResponseEntity.ok().body(new ResultMessageResponseDto("password를 성공적으로 변경하였습니다."));
    } catch (Exception e) {
      log.error("userId={}, step=비밀번호_재설정_실패, status=FAILED", user.getId(), e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("비밀번호를 재설정하는데 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(
      summary = "유저 정보 조회",
      description = "프로필에 존재하는 유저 정보를 조회하는 api"
  )
  @GetMapping()
  public ResponseEntity<GetUserInfoResponseDto> getUserInfo(@AuthenticationPrincipal User user){
    try {
      log.info("userId={}, step=유저정보_조회_시작, status=SUCCESS", user.getId());
      GetUserInfoResponseDto dto = userService.getUserInfo(user.getId());
      log.info("userId={}, step=유저정보_조회_완료, status=SUCCESS", user.getId());

      return ResponseEntity.ok().body(dto);
    } catch (Exception e) {
      log.error("userId={}, step=유저정보_조회_실패, status=FAILED", user.getId(), e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("유저 정보를 조회하는데 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(
      summary = "유저 정보 업데이트",
      description = "수정할 수 있는 유저 정보를 업데이트 하는 api"
  )
  @PatchMapping()
  public ResponseEntity<ResultMessageResponseDto> updateUserInfo(@RequestBody UpdateUserInfoRequestDto dto, @AuthenticationPrincipal User user){
    try {
      log.info("userId={}, step=유저정보_수정_시작, status=SUCCESS", user.getId());
      userService.updateUserInfo(user.getId(), dto);
      log.info("userId={}, step=유저정보_수정_완료, status=SUCCESS", user.getId());

      return ResponseEntity.ok().body(new ResultMessageResponseDto("정상적으로 수정되었습니다."));
    } catch (Exception e) {
      log.error("userId={}, step=유저정보_수정_실패, status=FAILED", user.getId(), e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("유저 정보를 수정하는데 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(summary = "친구초대코드 조회")
  @GetMapping("invitation-code")
  public ResponseEntity<GetFriendInvitationCodeRequestDto> getInvitationCode(@AuthenticationPrincipal User user){
    try {
      log.info("userId={}, step=친구초대코드_조회_시작, status=SUCCESS", user.getId());
      return ResponseEntity.ok().body(new GetFriendInvitationCodeRequestDto(user.getFriendInvitationCode()));
    } catch (Exception e) {
      log.error("userId={}, step=친구초대코드_조회_실패, status=FAILED", user.getId(), e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("친구초대코드를 조회하는데 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(summary = "친구초대코드 입력", description = "친구초대코드 입력 및 보상 받기")
  @PostMapping("invitation-code")
  public ResponseEntity<ResultMessageResponseDto> inputInvitationCodeAndGetReward(@AuthenticationPrincipal User user, @RequestParam String code){
    try {
      log.info("userId={}, step=친구초대코드_입력_시작, status=SUCCESS", user.getId());
      userService.checkFriendInvitationCodeAndProvideReward(user, code);
      log.info("userId={}, step=친구초대코드_입력_완료, status=SUCCESS", user.getId());

      return ResponseEntity.ok().body(new ResultMessageResponseDto("친구와 본인 모두 보상을 획득하였습니다."));
    } catch (Exception e) {
      log.error("userId={}, step=친구초대코드_입력_실패, status=FAILED", user.getId(), e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("친구초대코드 처리에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(summary = "휴면 계정을 업데이트합니다.",
      description = "계정을 휴면시키거나 해제시킵니다. 유저가 휴면 상태에 들어가면 이성추천에서 유저가 배제됩니다.")
  @PostMapping("dormant")
  public ResponseEntity<ResultMessageResponseDto> updateDormantAccount(
      @Parameter(description = "계정 휴면 혹은 휴면 해제",
        schema = @Schema(example = "DORMANT", allowableValues = {"DORMANT", "CANCEL"})
      )@RequestParam(value = "request") String request,

      @AuthenticationPrincipal User user){

    try {
      log.info("userId={}, request={}, step=휴면계정_상태변경_시작, status=SUCCESS", user.getId(), request);
      userService.updateDormantAccount(user, request);
      log.info("userId={}, request={}, step=휴면계정_상태변경_완료, status=SUCCESS", user.getId(), request);

      return ResponseEntity.ok().body(new ResultMessageResponseDto("휴면 계정 정보를 업데이트 하였습니다."));
    } catch (Exception e) {
      log.error("userId={}, request={}, step=휴면계정_상태변경_실패, status=FAILED", user.getId(), request, e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("휴면 계정 상태를 변경하는데 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(summary = "유저의 접근정보를 저장합니다.", description = "유저가 앱을 실행할 때 이 api를 호출합니다.")
  @PostMapping("access")
  public ResponseEntity<ResultMessageResponseDto> saveUserAccessLog(@AuthenticationPrincipal User user){
    if (user.getId() == null){
      log.info("step=유저_접근로그_저장, status=SKIPPED, reason=비로그인");
      return ResponseEntity.ok().body(new ResultMessageResponseDto("아직 로그인하지 않은 유저입니다."));
    }
    try {
      log.info("userId={}, step=유저_접근로그_저장_시작, status=SUCCESS", user.getId());
      UserAccessLog accessLog = userService.saveUserAccessLog(user);
      if (accessLog == null){
        log.info("userId={}, step=유저_접근로그_저장, status=SKIPPED, reason=중복저장", user.getId());
        return ResponseEntity.ok().body(new ResultMessageResponseDto("오늘 이미 접속했던 유저 입니다."));
      }
      log.info("userId={}, step=유저_접근로그_저장_완료, status=SUCCESS", user.getId());
      return ResponseEntity.ok().body(new ResultMessageResponseDto("유저 접속 정보가 저장되었습니다."));
    } catch (Exception e) {
      log.error("userId={}, step=유저_접근로그_저장_실패, status=FAILED", user.getId(), e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("유저 접속 정보를 저장하는데 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

}
