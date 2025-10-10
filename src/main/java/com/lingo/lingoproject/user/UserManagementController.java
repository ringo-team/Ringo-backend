package com.lingo.lingoproject.user;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserManagementController {

  private final UserService userService;

  @GetMapping()
  public ResponseEntity<?> getUsersInfo(@RequestParam int page, @RequestParam int size){
    List<GetUserInfoResponseDto> dtos = userService.getPageableUserInfo(page, size);
    return ResponseEntity.ok().body(dtos);
  }

  @PostMapping("/{id}/blocks")
  public ResponseEntity<?> blockUser(@RequestHeader(value = "token") String token, @PathVariable Long id){
    userService.blockUser(id, token);
    return ResponseEntity.ok().build();
  }
}
