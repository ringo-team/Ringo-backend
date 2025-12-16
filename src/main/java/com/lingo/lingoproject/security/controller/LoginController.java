package com.lingo.lingoproject.security.controller;


import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.exception.ErrorCode;
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
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@Tag(name = "인증", description = "로그인/로그아웃 및 토큰 관련 API")
public class LoginController {

  private final LoginService loginService;

  @Operation(
      summary = "로그인",
      description = "커스텀 인증 필터에서 처리한 로그인 정보를 사용해 토큰을 발급합니다."
  )
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "0000",
              description = "로그인 성공",
              content = @Content(schema = @Schema(implementation = LoginResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0003",
              description = "유저가 인증되지 않았습니다.",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0005",
              description = "유저 객체를 찾을 수 없습니다.",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E1000",
              description = "내부 오류, 기타 문의",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          )
      }
  )
  @PostMapping("/login")
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
      throw new RingoException(e.getMessage(), ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
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
      throw new RingoException("로그인에 실패하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(summary = "토큰 재발급", description = "리프레시 토큰을 사용해 새로운 액세스/리프레시 토큰을 발급합니다.")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "0000",
              description = "토큰 재발급 성공",
              content = @Content(schema = @Schema(implementation = LoginResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0005",
              description = "유저 객체를 찾을 수 없습니다.",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E1000",
              description = "내부 오류, 기타 문의",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          )
      }
  )
  @GetMapping("/refresh")
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
      throw new RingoException("토큰을 재발급하는데 실패하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(summary = "회원 가입", description = "회원 가입에 필요한 정보로 계정을 생성합니다.")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "0000",
              description = "회원가입 성공",
              content = @Content(schema = @Schema(implementation = LoginResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0007",
              description = "아이디 또는 비밀번호의 형식이 불일치합니다.",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0008",
              description = "중복된 아이디입니다.",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E1000",
              description = "내부 오류, 기타 문의",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          )
      }
  )
  @PostMapping("/signup")
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
      throw new RingoException("회원가입에 실패하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(summary = "회원 정보 입력", description = "회원가입 시 유저 정보 저장")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "0000",
              description = "회원정보 저장 성공",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0007",
              description = "잘못된 파라미터가 전달되었습니다.",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0005",
              description = "유저 객체를 찾을 수 없습니다.",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0015",
              description = "미성년자는 가입할 수 없습니다.",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0003",
              description = "본인인증되지 않은 회원입니다.",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0014",
              description = "블락된 유저입니다.",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E1000",
              description = "내부 오류, 기타 문의",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          )
      }
  )
  @PostMapping("/signup/user-info")
  public ResponseEntity<ResultMessageResponseDto> signupUserInfo(@Valid @RequestBody SignupUserInfoDto dto){
    try {
      log.info("step=회원_정보_저장_시작, status=SUCCESS");
      loginService.saveUserInfo(dto);
      log.info("step=회원_정보_저장_완료, status=SUCCESS");

      return ResponseEntity.status(HttpStatus.OK)
          .body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "유저 정보 저장이 완료되었습니다."));
    }catch (Exception e) {
      log.error("step=회원_정보_저장_실패, status=FAILED", e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("회원 정보 저장에 실패하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(summary = "아이디 중복 확인", description = "중복시 NOT_ACCEPTABLE(406) 반환 ")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "0000",
              description = "사용가능한 아이디",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0008",
              description = "중복된 아이디입니다.",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E1000",
              description = "내부 오류, 기타 문의",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          )
      }
  )
  @GetMapping("/signup/check-loginId")
  public ResponseEntity<ResultMessageResponseDto> verifyDuplicatedLoginId(@RequestParam(value = "email") String email)
  {
    try {
      log.info("step=회원_아이디_중복확인_시작, status=SUCCESS");
      boolean isDuplicated = loginService.verifyDuplicatedLoginId(email);
      log.info("step=회원_아이디_중복확인_완료, status=SUCCESS");
      if (isDuplicated) {
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
            .body(new ResultMessageResponseDto(ErrorCode.DUPLICATED.getCode(), "중복된 아이디입니다."));
      }
      return ResponseEntity.status(HttpStatus.OK)
          .body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "사용가능한 아이디 입니다."));
    }catch (Exception e) {
      log.error("step=회원_아이디_중복확인_실패, status=FAILED", e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("아이디 중복확인에 실패하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(summary = "닉네임 중복확인")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "0000",
              description = "사용가능한 닉네임",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0008",
              description = "중복된 닉네임입니다.",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E1000",
              description = "내부 오류, 기타 문의",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          )
      }
  )
  @GetMapping("/signup/check-nickname")
  public ResponseEntity<ResultMessageResponseDto> verifyDuplicatedNickname(@RequestParam(value = "nickname") String nickname){
    try{
      log.info("step=회원_닉네임_중복확인_시작, status=SUCCESS");
      boolean isDuplicated = loginService.verifyDuplicatedNickname(nickname);
      log.info("step=회원_닉네임_중복확인_완료, status=SUCCESS");
      if (isDuplicated) {
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
            .body(new ResultMessageResponseDto(ErrorCode.DUPLICATED.getCode(), "중복된 닉네임입니다."));
      }
      return ResponseEntity.status(HttpStatus.OK)
          .body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "사용가능한 닉네임입니다."));
    }catch (Exception e) {
      log.error("step=회원_닉네임_중복확인_실패, status=FAILED", e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("닉네임 중복확인에 실패하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(summary = "로그아웃", description = "헤더의 액세스 토큰을 무효화합니다.")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "0000",
              description = "로그아웃 성공",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E1000",
              description = "내부 오류, 기타 문의",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          )
      }
  )
  @GetMapping("/api/logout")
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
          .body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "로그아웃이 완료되었습니다."));
    }catch (Exception e) {
      log.error("userId={}, step=로그아웃_실패, status=FAILED", user.getId(), e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("로그아웃에 실패하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
