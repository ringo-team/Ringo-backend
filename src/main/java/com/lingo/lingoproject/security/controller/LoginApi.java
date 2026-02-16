package com.lingo.lingoproject.security.controller;

import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.security.controller.dto.LoginInfoDto;
import com.lingo.lingoproject.security.controller.dto.SignupResponseDto;
import com.lingo.lingoproject.security.controller.dto.SignupUserInfoDto;
import com.lingo.lingoproject.security.dto.LoginResponseDto;
import com.lingo.lingoproject.security.dto.RegenerateTokenResponseDto;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name="login-signup-controller", description = "로그인/로그아웃/회원가입 및 토큰 관련 API")
public interface LoginApi {

  @Operation(summary = "로그인", description = "커스텀 인증 필터에서 처리한 로그인 정보를 사용해 토큰을 발급합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "0000", description = "로그인 성공", content = @Content(schema = @Schema(implementation = LoginResponseDto.class))),
      @ApiResponse(responseCode = "E0003", description = "유저가 인증되지 않았습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E0005", description = "유저 객체를 찾을 수 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
  })
  @PostMapping("/login")
  ResponseEntity<LoginResponseDto> login(
      /*
       * swagger용 requestBody입니다.
       */
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "login 요청 dto, filter 단에서 값을 먼저 읽고 처리함",
          content =  @Content(schema = @Schema(implementation = LoginInfoDto.class))
      )LoginInfoDto loginInfoDto,
      HttpServletRequest request
  );

  @Operation(summary = "토큰 재발급", description = "리프레시 토큰을 사용해 새로운 액세스/리프레시 토큰을 발급합니다.")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "0000", description = "토큰 재발급 성공", content = @Content(schema = @Schema(implementation = LoginResponseDto.class))),
          @ApiResponse(responseCode = "E0005", description = "유저 객체를 찾을 수 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
  })
  @GetMapping("/refresh")
  ResponseEntity<RegenerateTokenResponseDto> refresh(
      @Parameter(description = "리프레시 토큰 값", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...") @RequestParam String refreshToken
  );

  @Operation(summary = "회원 가입", description = "회원 가입에 필요한 정보로 계정을 생성합니다.")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "0000", description = "회원가입 성공", content = @Content(schema = @Schema(implementation = LoginResponseDto.class))),
          @ApiResponse(responseCode = "E0007", description = "아이디 또는 비밀번호의 형식이 불일치합니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E0008", description = "중복된 아이디입니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
  })
  @PostMapping("/signup")
  ResponseEntity<SignupResponseDto> signup(@Valid @RequestBody LoginInfoDto dto);

  @Operation(summary = "회원 정보 입력", description = "회원가입 시 유저 정보 저장")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "0000", description = "회원정보 저장 성공", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E0007", description = "잘못된 파라미터가 전달되었습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E0005", description = "유저 객체를 찾을 수 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E0015", description = "미성년자는 가입할 수 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E0003", description = "본인인증되지 않은 회원입니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E0014", description = "블락된 유저입니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
  })
  @PostMapping("/signup/user-info")
  ResponseEntity<ResultMessageResponseDto> signupUserInfo(@Valid @RequestBody SignupUserInfoDto dto);

  @Operation(summary = "아이디 중복 확인", description = "중복시 NOT_ACCEPTABLE(406) 반환 ")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "0000", description = "사용가능한 아이디", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E0008", description = "중복된 아이디입니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
  })
  @GetMapping("/signup/check-loginId")
  ResponseEntity<ResultMessageResponseDto> verifyDuplicatedLoginId(@RequestParam(value = "loginId") String loginId);

  @Operation(summary = "닉네임 중복확인")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "0000", description = "사용가능한 닉네임", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E0008", description = "중복된 닉네임입니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
  })
  @GetMapping("/signup/check-nickname")
  ResponseEntity<ResultMessageResponseDto> verifyDuplicatedNickname(@RequestParam(value = "nickname") String nickname);


  @Operation(summary = "로그아웃", description = "헤더의 액세스 토큰을 무효화합니다.")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "0000", description = "로그아웃 성공", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
  })
  @GetMapping("/api/logout")
  ResponseEntity<ResultMessageResponseDto> logout(
      @AuthenticationPrincipal User user,
      @RequestHeader(value = "Authorization") String token
  );

}
