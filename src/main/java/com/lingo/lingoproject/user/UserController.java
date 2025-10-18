package com.lingo.lingoproject.user;

import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.user.dto.GetUserInfoResponseDto;
import com.lingo.lingoproject.user.dto.ResetPasswordRequestDto;
import com.lingo.lingoproject.user.dto.UpdateUserInfoRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
  public ResponseEntity<?> deleteUser(
      @Parameter(description = "유저id", example = "4")
      @PathVariable Long id,

      @AuthenticationPrincipal User user){
    Long userId = user.getId();
    if(!userId.equals(id)){
      throw new RingoException("쟐못된 접근입니다.", HttpStatus.FORBIDDEN);
    }
    userService.deleteUser(id);
    return ResponseEntity.ok().body("유저를 성공적으로 삭제하였습니다.");
  }

  @Operation(
      summary = "유저 id 찾기",
      description = "본인인증 성공한 유저의 id 찾기"
  )
  @GetMapping("/find-id")
  public ResponseEntity<?> findUserId(@AuthenticationPrincipal User user){
    String username = userService.findUserId(user.getId());
    return ResponseEntity.ok().body(username);
  }

  @Operation(
      summary = "유저 password 재설정",
      description = "본인인증 성공한 유저 password 재설정"
  )
  @PatchMapping("reset-password")
  public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequestDto dto, @AuthenticationPrincipal User user) throws Exception{
    userService.resetPassword(dto.password(), user.getId());
    return ResponseEntity.ok().body("password를 성공적으로 변경하였습니다.");
  }

  @Operation(
      summary = "유저 정보 조회",
      description = "프로필에 존재하는 유저 정보를 조회하는 api"
  )
  @GetMapping()
  public ResponseEntity<?> getUserInfo(@AuthenticationPrincipal User user){
    GetUserInfoResponseDto dto = userService.getUserInfo(user.getId());
    return ResponseEntity.ok().body(dto);
  }

  @Operation(
      summary = "유저 정보 업데이트",
      description = "수정할 수 있는 유저 정보를 업데이트 하는 api"
  )
  @PatchMapping()
  public ResponseEntity<?> updateUserInfo(@RequestBody UpdateUserInfoRequestDto dto, @AuthenticationPrincipal User user){
    userService.updateUserInfo(user.getId(), dto);
    return ResponseEntity.ok().body("정상적으로 수정되었습니다.");
  }
}
