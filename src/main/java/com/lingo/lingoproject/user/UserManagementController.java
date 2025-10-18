package com.lingo.lingoproject.user;

import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.domain.enums.Role;
import com.lingo.lingoproject.user.dto.GetUserInfoResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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
  @PostMapping("/{id}/blocks")
  public ResponseEntity<?> blockUser(
      @Parameter(description = "블락시키고자 하는 유저 id", example = "5")
      @PathVariable Long id,

      @AuthenticationPrincipal User admin){
    userService.blockUser(id, admin.getId());
    return ResponseEntity.ok().build();
  }

  @Operation(
      summary = "유저 영구 삭제",
      description = "유저와 관련된 모든 정보를 없애거나 관계를 끊어버린다."
  )
  @DeleteMapping("/{id}")
  public ResponseEntity<?> deleteUser(
      @Parameter(description = "삭제하고자 하는 유저id")
      @PathVariable Long id){
    userService.deleteUser(id);
    return ResponseEntity.ok().build();
  }
}
