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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class LoginController implements LoginApi{

  private final LoginService loginService;

  public ResponseEntity<LoginResponseDto> login(LoginInfoDto loginInfoDto, HttpServletRequest request) {
    LoginInfoDto info = null;

    try {
      info = (LoginInfoDto) request.getAttribute("requestBody");
    } catch (Exception e) {
      log.error("step=로그인_요청값_역직렬화_실패, status=FAILED", e);
      throw new RingoException(e.getMessage(), ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    log.info("step=로그인_시작, status=SUCCESS");
    LoginResponseDto response = loginService.login(info);
    log.info("step=로그인_완료, status=SUCCESS");

    return ResponseEntity.status(HttpStatus.OK).body(response);
  }


  public ResponseEntity<RegenerateTokenResponseDto> refresh(String refreshToken) {
    log.info("step=토큰_재발급_시작, status=SUCCESS");
    RegenerateTokenResponseDto response = loginService.regenerateToken(refreshToken);
    log.info("step=토큰_재발급_완료, status=SUCCESS");

    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  public ResponseEntity<SignupResponseDto> signup(LoginInfoDto dto) {
    log.info("step=회원가입_시작, status=SUCCESS");
    User user = loginService.signup(dto);
    log.info("step=회원가입_완료, status=SUCCESS");

    return ResponseEntity.status(HttpStatus.OK).body(new SignupResponseDto(user.getId(), "회원가입이 완료되었습니다."));
  }


  public ResponseEntity<ResultMessageResponseDto> signupUserInfo(@Valid @RequestBody SignupUserInfoDto dto){
    log.info("step=회원_정보_저장_시작, status=SUCCESS");
    loginService.saveUserInfo(dto);
    log.info("step=회원_정보_저장_완료, status=SUCCESS");

    return ResponseEntity.status(HttpStatus.OK)
        .body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "유저 정보 저장이 완료되었습니다."));
  }


  public ResponseEntity<ResultMessageResponseDto> verifyDuplicatedLoginId(String loginId)
  {
    log.info("step=회원_아이디_중복확인_시작, status=SUCCESS");
    boolean isDuplicated = loginService.verifyDuplicatedLoginId(loginId);
    log.info("step=회원_아이디_중복확인_완료, status=SUCCESS");
    if (isDuplicated) {
      return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
          .body(new ResultMessageResponseDto(ErrorCode.DUPLICATED.getCode(), "중복된 아이디입니다."));
    }
    return ResponseEntity.status(HttpStatus.OK)
        .body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "사용가능한 아이디 입니다."));
  }


  public ResponseEntity<ResultMessageResponseDto> verifyDuplicatedNickname(String nickname){
    log.info("step=회원_닉네임_중복확인_시작, status=SUCCESS");
    boolean isDuplicated = loginService.verifyDuplicatedNickname(nickname);
    log.info("step=회원_닉네임_중복확인_완료, status=SUCCESS");
    if (isDuplicated) {
      return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
          .body(new ResultMessageResponseDto(ErrorCode.DUPLICATED.getCode(), "중복된 닉네임입니다."));
    }
    return ResponseEntity.status(HttpStatus.OK)
        .body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "사용가능한 닉네임입니다."));
  }

  public ResponseEntity<ResultMessageResponseDto> logout(HttpServletRequest request) {
    loginService.logout(request);

    return ResponseEntity.status(HttpStatus.OK)
        .body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "로그아웃이 완료되었습니다."));
  }
}
