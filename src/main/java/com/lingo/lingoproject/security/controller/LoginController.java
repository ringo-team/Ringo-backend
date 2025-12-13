package com.lingo.lingoproject.security.controller;


import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.security.controller.dto.LoginInfoDto;
import com.lingo.lingoproject.security.controller.dto.SignupResponseDto;
import com.lingo.lingoproject.security.controller.dto.SignupUserInfoDto;
import com.lingo.lingoproject.security.dto.LoginResponseDto;
import com.lingo.lingoproject.security.dto.RegenerateTokenResponseDto;
import com.lingo.lingoproject.security.services.LoginService;
import com.lingo.lingoproject.utils.ResultMessageResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
  @Operation(
      summary = "로그인",
      description = "커스텀 인증 필터에서 처리한 로그인 정보를 사용해 토큰을 발급합니다."
  )
  public ResponseEntity<LoginResponseDto> login(
      /*
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
      log.error("step=로그인_요청값_역직렬화_실패, status=FAILED", e);
      throw new RingoException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
    try {
      log.info("step=로그인_시작, status=SUCCESS");
      LoginResponseDto response = loginService.login(info);
      log.info("step=로그인_완료, status=SUCCESS");
      return ResponseEntity.status(HttpStatus.OK)
          .body(response);
    } catch (Exception e) {
      log.error("step=로그인_실패, status=FAILED", e);
      if (e instanceof RingoException re){throw re;}
      throw new RingoException("로그인에 실패하였습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @GetMapping("/refresh")
  @Operation(summary = "토큰 재발급", description = "리프레시 토큰을 사용해 새로운 액세스/리프레시 토큰을 발급합니다.")
  public ResponseEntity<RegenerateTokenResponseDto> refresh(
      @Parameter(
          description = "리프레시 토큰 값",
          example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
      )
      @RequestParam String refreshToken
  ) {
    try {
      log.info("step=토큰_재발급_시작, status=SUCCESS");
      RegenerateTokenResponseDto response = loginService.regenerateToken(refreshToken);
      log.info("step=토큰_재발급_완료, status=SUCCESS");

      return ResponseEntity.status(HttpStatus.OK)
          .body(response);
    } catch (Exception e) {
      log.error("step=토큰_재발급_실패, status=FAILED", e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("토큰을 재발급하는데 실패하였습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @PostMapping("/signup")
  @Operation(summary = "회원 가입", description = "회원 가입에 필요한 정보로 계정을 생성합니다.")
  public ResponseEntity<SignupResponseDto> signup(@Valid @RequestBody LoginInfoDto dto) {
    try {
      log.info("step=회원가입_시작, status=SUCCESS");
      User user = loginService.signup(dto);
      log.info("step=회원가입_완료, status=SUCCESS");
      return ResponseEntity.status(HttpStatus.OK).body(new SignupResponseDto(user.getId(), "회원가입이 완료되었습니다."));
    } catch (Exception e) {
      log.error("step=회원가입_실패, status=FAILED", e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("회원가입에 실패하였습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @PostMapping("/signup/user-info")
  @Operation(summary = "회원 정보 입력", description = "회원가입 시 유저 정보 저장")
  public ResponseEntity<ResultMessageResponseDto> signupUserInfo(@Valid @RequestBody SignupUserInfoDto dto){
    try {
      log.info("step=회원_정보_저장_시작, status=SUCCESS");
      loginService.saveUserInfo(dto);
      log.info("step=회원_정보_저장_완료, status=SUCCESS");
      return ResponseEntity.ok().body(new ResultMessageResponseDto("유저 정보 저장이 완료되었습니다."));
    }catch (Exception e) {
      log.error("step=회원_정보_저장_실패, status=FAILED", e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("회원 정보 저장에 실패하였습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @GetMapping("signup/check-loginId")
  @Operation(summary = "아이디 중복 확인", description = "중복시 NOT_ACCEPTABLE(406) 반환 ")
  public ResponseEntity<ResultMessageResponseDto> verifyDuplicatedLoginId(@RequestParam(value = "email") String email)
  {
    try {
      log.info("step=회원_중복확인_시작, status=SUCCESS");
      boolean isDuplicated = loginService.verifyDuplicatedLoginId(email);
      if (isDuplicated) {
        log.info("step=회원_중복확인_완료, status=SUCCESS");
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
            .body(new ResultMessageResponseDto("중복된 아이디입니다."));
      }
      log.info("step=회원_중복확인_완료, status=SUCCESS");
      return ResponseEntity.status(HttpStatus.OK)
          .body(new ResultMessageResponseDto("사용가능한 아이디 입니다."));
    }catch (Exception e) {
      log.error("step=아이디_중복확인_실패, status=FAILED", e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("아이디 중복확인에 실패하였습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @GetMapping("/api/logout")
  @Operation(summary = "로그아웃", description = "헤더의 액세스 토큰을 무효화합니다.")
  public ResponseEntity<ResultMessageResponseDto> logout(
      HttpServletRequest request,
      @AuthenticationPrincipal User user,
      @RequestHeader(value = "Authorization") String token
  ) {
    try {
      log.info("userId={}, step=로그아웃_시작, status=SUCCESS", user.getId());
      request.getSession().invalidate();
      loginService.logout(user, token);
      log.info("userId={}, step=로그아웃_완료, status=SUCCESS", user.getId());
      return ResponseEntity.status(HttpStatus.OK)
          .body(new ResultMessageResponseDto("로그아웃이 완료되었습니다."));
    }catch (Exception e) {
      log.error("userId={}, step=로그아웃_실패, status=FAILED", user.getId(), e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("로그아웃에 실패하였습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
