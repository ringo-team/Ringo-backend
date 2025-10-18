package com.lingo.lingoproject.security.controller;


import com.lingo.lingoproject.access.annotation.AccessLog;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.security.dto.LoginResponseDto;
import com.lingo.lingoproject.security.services.LoginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@ApiResponse(
    responseCode = "200",
    description = "성공"
)
@Tag(name = "인증", description = "로그인/로그아웃 및 토큰 관련 API")
public class LoginController {

  private final LoginService loginService;

  @PostMapping("/login")
  @AccessLog
  @Operation(
      summary = "로그인",
      description = "커스텀 인증 필터에서 처리한 로그인 정보를 사용해 토큰을 발급합니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "401 (UNAUTHORIZED)",
          description = "토큰 재발급을 요청합니다."
      ),
      @ApiResponse(
          responseCode = "403 (FORBIDDEN)",
          description = "자격 증명이 올바르지 않아 접근을 차단합니다."
      )
  })
  public ResponseEntity<LoginResponseDto> login(
      /**
       * swagger용 requestBody입니다.
       */
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "login 요청 dto, filter 단에서 값을 먼저 읽고 처리함",
          content =  @Content(schema = @Schema(implementation = LoginInfoDto.class))
      )LoginInfoDto loginInfoDto,
      HttpServletRequest request
  ) {
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
  @AccessLog
  @Operation(summary = "토큰 재발급", description = "리프레시 토큰을 사용해 새로운 액세스/리프레시 토큰을 발급합니다.")
  @ApiResponse(
      responseCode = "500",
      description = "토큰 재발급 처리 중 서버 오류가 발생했습니다."
  )
  public ResponseEntity<LoginResponseDto> refresh(
      @Parameter(
          description = "리프레시 토큰 값",
          example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
      )
      @RequestParam String refreshToken
  ) {
    LoginResponseDto response = null;
    try {
      response = loginService.regenerateToken(refreshToken);
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
    return ResponseEntity.status(HttpStatus.OK)
        .body(response);
  }

  @PostMapping("/signup")
  @Operation(summary = "회원 가입", description = "회원 가입에 필요한 정보로 계정을 생성합니다.")
  public ResponseEntity<String> signup(@RequestBody LoginInfoDto dto) {
    loginService.signup(dto);
    return ResponseEntity.status(HttpStatus.OK).body("회원가입이 완료되었습니다.");
  }

  @GetMapping("/api/logout")
  @Operation(summary = "로그아웃", description = "헤더의 액세스 토큰을 무효화합니다.")
  public ResponseEntity<String> logout(@AuthenticationPrincipal User user, @RequestHeader(value = "Authorization") String token) {
    log.info(user.getId().toString());
    loginService.logout(user, token);
    return ResponseEntity.status(HttpStatus.OK).body("로그아웃이 완료되었습니다.");
  }
}
