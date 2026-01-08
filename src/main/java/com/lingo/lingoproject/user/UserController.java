package com.lingo.lingoproject.user;

import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.exception.ErrorCode;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.user.dto.GetFriendInvitationCodeResponseDto;
import com.lingo.lingoproject.user.dto.GetUserInfoResponseDto;
import com.lingo.lingoproject.user.dto.GetUserLoginIdResponseDto;
import com.lingo.lingoproject.user.dto.ResetPasswordRequestDto;
import com.lingo.lingoproject.user.dto.UpdateUserInfoRequestDto;
import com.lingo.lingoproject.utils.ResultMessageResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "0000",
              description = "회원 탈퇴 성공",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0003",
              description = "유저를 탈퇴할 권한이 없습니다.",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E1000",
              description = "내부 오류, 기타 문의",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          )
      }
  )
  @DeleteMapping("/{userId}")
  public ResponseEntity<ResultMessageResponseDto> deleteUser(
      @Parameter(description = "유저id", example = "4")
      @PathVariable Long userId,

      @Parameter(description = "유저 탈퇴 사유", example = "좋은 인연을 만날 수 없어서")
      @NotBlank
      @RequestParam(value = "reason") String reason,

      @AuthenticationPrincipal User user
  ){
    Long id = user.getId();

    try {
      // 유저 권한 체크
      if(!id.equals(userId)){
        throw new RingoException("유저를 탈퇴할 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
      }

      // 유저 삭제
      log.info("userId={}, step=회원탈퇴_시작, status=SUCCESS", userId);
      userService.deleteUser(user, reason);
      log.info("userId={}, step=회원탈퇴_완료, status=SUCCESS", userId);

      return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(
          ErrorCode.SUCCESS.getCode(), "유저를 성공적으로 삭제하였습니다."));
    } catch (Exception e) {
      log.error("userId={}, step=회원탈퇴_실패, status=FAILED", userId, e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("회원 탈퇴에 실패했습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(
      summary = "유저 id 찾기",
      description = "본인인증 성공한 유저의 id 찾기"
  )
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "0000",
              description = "조회 성공",
              content = @Content(schema = @Schema(implementation = GetUserLoginIdResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0003",
              description = "아이디를 조회할 권한이 없습니다.",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E1000",
              description = "내부 오류, 기타 문의",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          )
      }
  )
  @GetMapping("/find-id")
  public ResponseEntity<GetUserLoginIdResponseDto> findUserLoginId(@AuthenticationPrincipal User user){
    try {
      log.info("userId={}, step=유저_ID찾기_시작, status=SUCCESS", user.getId());
      String loginId = userService.findUserLoginId(user);
      log.info("userId={}, step=유저_ID찾기_완료, status=SUCCESS", user.getId());

      return ResponseEntity.status(HttpStatus.OK).body(new GetUserLoginIdResponseDto(loginId));
    } catch (Exception e) {
      log.error("userId={}, step=유저_ID찾기_실패, status=FAILED", user.getId(), e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("유저 아이디 조회에 실패했습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(
      summary = "유저 password 재설정",
      description = "본인인증 성공한 유저 password 재설정"
  )
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "0000",
              description = "재설정 성공",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0003",
              description = "비밀번호를 재설정할 권한이 없습니다.",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E1000",
              description = "내부 오류, 기타 문의",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          )
      }
  )
  @PatchMapping("reset-password")
  public ResponseEntity<ResultMessageResponseDto> resetPassword(@RequestBody ResetPasswordRequestDto dto, @AuthenticationPrincipal User user){
    try {
      log.info("userId={}, step=비밀번호_재설정_시작, status=SUCCESS", user.getId());
      userService.resetPassword(dto.password(), user);
      log.info("userId={}, step=비밀번호_재설정_완료, status=SUCCESS", user.getId());

      return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(
          ErrorCode.SUCCESS.getCode(), "password를 성공적으로 변경하였습니다."));
    } catch (Exception e) {
      log.error("userId={}, step=비밀번호_재설정_실패, status=FAILED", user.getId(), e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("비밀번호를 재설정하는데 실패했습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(
      summary = "유저 정보 조회",
      description = "프로필에 존재하는 유저 정보를 조회하는 api"
  )
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "0000",
              description = "조회 성공",
              content = @Content(schema = @Schema(implementation = GetUserInfoResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E1000",
              description = "내부 오류, 기타 문의",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          )
      }
  )
  @GetMapping()
  public ResponseEntity<GetUserInfoResponseDto> getUserInfo(@AuthenticationPrincipal User user){
    try {
      log.info("userId={}, step=유저정보_조회_시작, status=SUCCESS", user.getId());
      GetUserInfoResponseDto dto = userService.getUserInfo(user);
      log.info("userId={}, step=유저정보_조회_완료, status=SUCCESS", user.getId());

      return ResponseEntity.status(HttpStatus.OK).body(dto);
    } catch (Exception e) {
      log.error("userId={}, step=유저정보_조회_실패, status=FAILED", user.getId(), e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("유저 정보를 조회하는데 실패했습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(
      summary = "유저 정보 업데이트",
      description = "수정할 수 있는 유저 정보를 업데이트 하는 api"
  )
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "0000",
              description = "업데이트 성공",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E1000",
              description = "내부 오류, 기타 문의",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          )
      }
  )
  @PatchMapping()
  public ResponseEntity<ResultMessageResponseDto> updateUserInfo(@RequestBody UpdateUserInfoRequestDto dto, @AuthenticationPrincipal User user){
    try {
      log.info("userId={}, step=유저정보_수정_시작, status=SUCCESS", user.getId());
      userService.updateUserInfo(user, dto);
      log.info("userId={}, step=유저정보_수정_완료, status=SUCCESS", user.getId());

      return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(
          ErrorCode.SUCCESS.getCode(), "정상적으로 수정되었습니다."));
    } catch (Exception e) {
      log.error("userId={}, step=유저정보_수정_실패, status=FAILED", user.getId(), e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("유저 정보를 수정하는데 실패했습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(summary = "친구초대코드 조회")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "0000",
              description = "조회 성공",
              content = @Content(schema = @Schema(implementation = GetFriendInvitationCodeResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E1000",
              description = "내부 오류, 기타 문의",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          )
      }
  )
  @GetMapping("invitation-code")
  public ResponseEntity<GetFriendInvitationCodeResponseDto> getInvitationCode(@AuthenticationPrincipal User user){
    try {
      log.info("userId={}, step=친구초대코드_조회_시작, status=SUCCESS", user.getId());
      return ResponseEntity.status(HttpStatus.OK).body(new GetFriendInvitationCodeResponseDto(user.getFriendInvitationCode()));
    } catch (Exception e) {
      log.error("userId={}, step=친구초대코드_조회_실패, status=FAILED", user.getId(), e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("친구초대코드를 조회하는데 실패했습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(summary = "친구초대코드 입력", description = "친구초대코드 입력 및 보상 받기")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "0000",
              description = "입력 성공",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0008",
              description = "초대 코드 입력횟수를 초과하였습니다.",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0001",
              description = "잘못된 코드를 입력하였습니다.",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E1000",
              description = "내부 오류, 기타 문의",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          )
      }
  )
  @PostMapping("invitation-code")
  public ResponseEntity<ResultMessageResponseDto> inputInvitationCodeAndGetReward(@AuthenticationPrincipal User user, @RequestParam String code){
    try {
      log.info("userId={}, step=친구초대코드_입력_시작, status=SUCCESS", user.getId());
      userService.checkFriendInvitationCodeAndProvideReward(user, code);
      log.info("userId={}, step=친구초대코드_입력_완료, status=SUCCESS", user.getId());

      return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(
          ErrorCode.SUCCESS.getCode(), "친구와 본인 모두 보상을 획득하였습니다."));
    } catch (Exception e) {
      log.error("userId={}, step=친구초대코드_입력_실패, status=FAILED", user.getId(), e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("친구초대코드 처리에 실패했습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(summary = "휴면 계정을 업데이트합니다.",
      description = "계정을 휴면시키거나 해제시킵니다. 유저가 휴면 상태에 들어가면 이성추천에서 유저가 배제됩니다.")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "0000",
              description = "업데이트 성공",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E1000",
              description = "내부 오류, 기타 문의",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          )
      }
  )
  @PostMapping("dormant")
  public ResponseEntity<ResultMessageResponseDto> updateDormantAccount(
      @AuthenticationPrincipal User user){

    try {
      log.info("userId={}, step=휴면계정_상태변경_시작, status=SUCCESS", user.getId());
      userService.updateDormantAccount(user);
      log.info("userId={}, step=휴면계정_상태변경_완료, status=SUCCESS", user.getId());

      return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(
          ErrorCode.SUCCESS.getCode(), "휴면 계정 정보를 업데이트 하였습니다."));
    } catch (Exception e) {
      log.error("userId={}, step=휴면계정_상태변경_실패, status=FAILED", user.getId(), e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("휴면 계정 상태를 변경하는데 실패했습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(summary = "유저의 접근정보를 저장합니다.", description = "유저가 앱을 실행할 때 이 api를 호출합니다.")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "0000",
              description = "저장 성공",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E1000",
              description = "내부 오류, 기타 문의",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          )
      }
  )
  @PostMapping("access")
  public ResponseEntity<ResultMessageResponseDto> saveUserAccessLog(@AuthenticationPrincipal User user){

    try {
      log.info("userId={}, step=유저_접근로그_저장_시작, status=SUCCESS", user.getId());
      userService.saveUserAccessLog(user);
      log.info("userId={}, step=유저_접근로그_저장_완료, status=SUCCESS", user.getId());

      return ResponseEntity.status(HttpStatus.OK)
          .body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "유저 접속 정보가 저장되었습니다."));
    } catch (Exception e) {
      log.error("userId={}, step=유저_접근로그_저장_실패, status=FAILED", user.getId(), e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("유저 접속 정보를 저장하는데 실패했습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

}
