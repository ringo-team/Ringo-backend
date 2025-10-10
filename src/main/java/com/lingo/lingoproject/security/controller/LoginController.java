package com.lingo.lingoproject.security.controller;


import com.lingo.lingoproject.security.response.LoginResponseDto;
import com.lingo.lingoproject.security.services.LoginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "성공"
    ),
    @ApiResponse(
        responseCode = "401 (UNAUTHORIZED)",
        description = "토큰 재발급을 요청합니다."
    ),
    @ApiResponse(
        responseCode = "403 (FORBIDDEN)",
        description = "자격 증명이 올바르지 않아 접근을 차단합니다."
    ),
    @ApiResponse(
        responseCode = "500",
        description = "토큰 재발급 처리 중 서버 오류가 발생했습니다."
    )
})
@Tag(name = "인증", description = "로그인/로그아웃 및 토큰 관련 API")
public class LoginController {

  private final LoginService loginService;

  @PostMapping
  @Operation(
      summary = "로그인",
      description = "커스텀 인증 필터에서 처리한 로그인 정보를 사용해 토큰을 발급합니다."
  )
  public ResponseEntity<?> login(HttpServletRequest request) {
    LoginInfoDto info = null;

    try {
      info = (LoginInfoDto) request.getAttribute("requestBody");
    } catch (Exception e) {
      e.printStackTrace();
    }
    LoginResponseDto response = loginService.login(info);
    return ResponseEntity.status(HttpStatus.OK)
        .body(response);
  }

  @GetMapping("/refresh")
  @Operation(summary = "토큰 재발급", description = "리프레시 토큰을 사용해 새로운 액세스/리프레시 토큰을 발급합니다.")
  @Parameter(
      description = "리프레시 토큰 값",
      example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  )
  public ResponseEntity<?> refresh(@RequestParam String refreshToken) {
    LoginResponseDto response = null;
    try {
      response = loginService.regenerateToken(refreshToken);
    } catch (NotFoundException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
    return ResponseEntity.status(HttpStatus.OK)
        .body(response);
  }

  @PostMapping("/signup")
  @Operation(summary = "회원 가입", description = "회원 가입에 필요한 정보로 계정을 생성합니다.")
  public ResponseEntity<?> signup(@RequestBody LoginInfoDto dto) {
    log.info(dto.toString());
    loginService.signup(dto);
    return ResponseEntity.status(HttpStatus.OK).build();
  }

  @PostMapping("/logout")
  @Operation(summary = "로그아웃", description = "헤더의 액세스 토큰을 무효화합니다.")
  public ResponseEntity<?> logout(@RequestHeader(value = "token") String token) {
    String accessToken = token.substring(7);
    loginService.logout(accessToken);
    return ResponseEntity.status(HttpStatus.OK).build();
  }
}
