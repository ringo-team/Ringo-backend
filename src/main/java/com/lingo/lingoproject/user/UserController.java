package com.lingo.lingoproject.user;

import com.amazonaws.Response;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @DeleteMapping("/{id}")
  public ResponseEntity<?> deleteUser(@PathVariable Long id){
    userService.deleteUser(id);
    return ResponseEntity.ok().body("유저를 성공적으로 삭제하였습니다.");
  }

  @GetMapping("/find-id")
  public ResponseEntity<?> findUserId(@RequestHeader(value = "token") String token) throws Exception{
    String username = userService.findUserId(token);
    return ResponseEntity.ok().body(username);
  }

  @PatchMapping("reset-password")
  public ResponseEntity<?> resetPassword(@RequestHeader(value = "token") String token, @RequestBody ResetPasswordRequestDto dto) throws Exception{
    userService.resetPassword(dto.password(), token);
    return ResponseEntity.ok().body("password를 성공적으로 변경하였습니다.");
  }

  @GetMapping()
  public ResponseEntity<?> getUserInfo(@RequestHeader(value = "token") String token){
    GetUserInfoResponseDto dto = userService.getUserInfo(token);
    return ResponseEntity.ok().body(dto);
  }

  @PatchMapping()
  public ResponseEntity<?> updateUserInfo(@RequestHeader(value = "token") String token, @RequestBody UpdateUserInfoRequestDto dto){
    userService.updateUserInfo(token, dto);
    return ResponseEntity.ok().body("정상적으로 수정되었습니다.");
  }
}
