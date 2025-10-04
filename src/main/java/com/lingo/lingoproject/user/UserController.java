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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @DeleteMapping("/{id}")
  public ResponseEntity<?> deleteUser(@PathVariable Long id){
    userService.deleteUser(id);
    return ResponseEntity.ok().body("유저를 성공적으로 삭제하였습니다.");
  }

  @GetMapping("/find-id")
  public ResponseEntity<?> findUserId(HttpSession session) throws Exception{
    String username = userService.findUserId(session);
    return ResponseEntity.ok().body(username);
  }

  @PatchMapping("reset-password")
  public ResponseEntity<?> resetPassword(HttpSession session, @RequestBody ResetPasswordRequestDto dto) throws Exception{
    userService.resetPassword(dto.password(), session);
    return ResponseEntity.ok().body("password를 성공적으로 변경하였습니다.");
  }
}
