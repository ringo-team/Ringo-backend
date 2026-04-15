package com.lingo.lingoproject.api.user;

import com.lingo.lingoproject.db.domain.User;
import com.lingo.lingoproject.common.exception.ErrorCode;
import com.lingo.lingoproject.common.exception.RingoException;
import com.lingo.lingoproject.api.user.dto.LoginInfoDto;
import com.lingo.lingoproject.api.user.dto.SignupResponseDto;
import com.lingo.lingoproject.api.user.dto.SignupUserInfoDto;
import com.lingo.lingoproject.common.security.dto.LoginResponseDto;
import com.lingo.lingoproject.common.security.dto.RegenerateTokenResponseDto;
import com.lingo.lingoproject.api.user.dto.GetFriendInvitationCodeResponseDto;
import com.lingo.lingoproject.api.user.dto.GetUserInfoResponseDto;
import com.lingo.lingoproject.api.user.dto.GetUserLoginIdResponseDto;
import com.lingo.lingoproject.api.user.dto.ResetPasswordRequestDto;
import com.lingo.lingoproject.api.user.dto.SaveMembershipRequestDto;
import com.lingo.lingoproject.api.user.dto.UpdateUserInfoRequestDto;
import com.lingo.lingoproject.common.utils.ResultMessageResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping()
@Slf4j
@RequiredArgsConstructor
public class UserController implements UserApi{

  private final UserService userService;

  public ResponseEntity<LoginResponseDto> login(
      LoginInfoDto loginInfoDto,
      HttpServletRequest request,
      User user
  ) {
    log.info("""

        step=로그인_시작,
        status=SUCCESS

        """);
    LoginResponseDto response = userService.login(user);
    log.info("""

        step=로그인_완료,
        status=SUCCESS

        """);

    return ResponseEntity.status(HttpStatus.OK).body(response);
  }


  public ResponseEntity<RegenerateTokenResponseDto> refresh(String refreshToken) {
    log.info("""

        step=토큰_재발급_시작,
        status=SUCCESS

        """);
    RegenerateTokenResponseDto response = userService.regenerateToken(refreshToken);
    log.info("""

        step=토큰_재발급_완료,
        status=SUCCESS

        """);

    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  public ResponseEntity<SignupResponseDto> signup(LoginInfoDto dto) {
    log.info("""

        step=회원가입_시작,
        status=SUCCESS

        """);
    User user = userService.signup(dto);
    log.info("""

        step=회원가입_완료,
        status=SUCCESS

        """);

    return ResponseEntity.status(HttpStatus.OK).body(new SignupResponseDto(user.getId(), "회원가입이 완료되었습니다."));
  }


  public ResponseEntity<ResultMessageResponseDto> signupUserInfo(@Valid @RequestBody SignupUserInfoDto dto){
    log.info("""

        step=회원_정보_저장_시작,
        status=SUCCESS

        """);
    userService.saveUserInfo(dto);
    log.info("""

        step=회원_정보_저장_완료,
        status=SUCCESS

        """);

    return ResponseEntity.status(HttpStatus.OK)
        .body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "유저 정보 저장이 완료되었습니다."));
  }


  public ResponseEntity<ResultMessageResponseDto> verifyDuplicatedLoginId(String loginId)
  {
    log.info("""

        step=회원_아이디_중복확인_시작,
        status=SUCCESS

        """);
    boolean isDuplicated = userService.verifyDuplicatedLoginId(loginId);
    log.info("""

        step=회원_아이디_중복확인_완료,
        status=SUCCESS

        """);
    if (isDuplicated) {
      return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
          .body(new ResultMessageResponseDto(ErrorCode.DUPLICATED.getCode(), "중복된 아이디입니다."));
    }
    return ResponseEntity.status(HttpStatus.OK)
        .body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "사용가능한 아이디 입니다."));
  }


  public ResponseEntity<ResultMessageResponseDto> verifyDuplicatedNickname(String nickname){
    log.info("""

        step=회원_닉네임_중복확인_시작,
        status=SUCCESS

        """);
    boolean isDuplicated = userService.verifyDuplicatedNickname(nickname);
    log.info("""

        step=회원_닉네임_중복확인_완료,
        status=SUCCESS

        """);
    if (isDuplicated) {
      return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
          .body(new ResultMessageResponseDto(ErrorCode.DUPLICATED.getCode(), "중복된 닉네임입니다."));
    }
    return ResponseEntity.status(HttpStatus.OK)
        .body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "사용가능한 닉네임입니다."));
  }

  public ResponseEntity<ResultMessageResponseDto> logout(HttpServletRequest request) {
    userService.logout(request);

    return ResponseEntity.status(HttpStatus.OK)
        .body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "로그아웃이 완료되었습니다."));
  }

  @Override
  public ResponseEntity<ResultMessageResponseDto> deleteUser(
      Long userId,
      String reason,
      String feedback,
      User user
  ){
    Long id = user.getId();

    if(!id.equals(userId)){
      throw new RingoException("유저를 탈퇴할 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
    }

    // 유저 삭제
    log.info("""

        userId={},
        step=회원탈퇴_시작,
        status=SUCCESS

        """, userId);
    userService.deleteUser(user, reason, feedback);
    log.info("""

        userId={},
        step=회원탈퇴_완료,
        status=SUCCESS

        """, userId);

    return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(
        ErrorCode.SUCCESS.getCode(), "유저를 성공적으로 삭제하였습니다."));
  }

  @Override
  public ResponseEntity<GetUserLoginIdResponseDto> findUserLoginId(User user){
    log.info("""

        userId={},
        step=유저_ID찾기_시작,
        status=SUCCESS

        """, user.getId());
    String loginId = userService.findUserLoginId(user);
    log.info("""

        userId={},
        step=유저_ID찾기_완료,
        status=SUCCESS

        """, user.getId());

    return ResponseEntity.status(HttpStatus.OK).body(new GetUserLoginIdResponseDto(loginId));
  }

  @Override
  public ResponseEntity<ResultMessageResponseDto> resetPassword(ResetPasswordRequestDto dto, User user){
    log.info("""

        userId={},
        step=비밀번호_재설정_시작,
        status=SUCCESS

        """, user.getId());
    userService.resetPassword(dto.password(), user);
    log.info("""

        userId={},
        step=비밀번호_재설정_완료,
        status=SUCCESS

        """, user.getId());

    return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(
        ErrorCode.SUCCESS.getCode(), "password를 성공적으로 변경하였습니다."));
  }

  @Override
  public ResponseEntity<GetUserInfoResponseDto> getUserInfo(Long userId, User user){
    log.info("""

        userId={},
        step=유저정보_조회_시작,
        status=SUCCESS

        """, user.getId());
    GetUserInfoResponseDto dto = userService.getUserInfo(userId, user);
    log.info("""

        userId={},
        step=유저정보_조회_완료,
        status=SUCCESS

        """, user.getId());

    return ResponseEntity.status(HttpStatus.OK).body(dto);
  }

  @Override
  public ResponseEntity<ResultMessageResponseDto> updateUserInfo(UpdateUserInfoRequestDto dto, User user){
    log.info("""

        userId={},
        step=유저정보_수정_시작,
        status=SUCCESS

        """, user.getId());
    userService.updateUserInfo(user, dto);
    log.info("""

        userId={},
        step=유저정보_수정_완료,
        status=SUCCESS

        """, user.getId());

    return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(
        ErrorCode.SUCCESS.getCode(), "정상적으로 수정되었습니다."));
  }

  @Override
  public ResponseEntity<GetFriendInvitationCodeResponseDto> getInvitationCode(User user){
    log.info("""

        userId={},
        step=친구초대코드_조회_시작,
        status=SUCCESS

        """, user.getId());
    return ResponseEntity.status(HttpStatus.OK).body(new GetFriendInvitationCodeResponseDto(user.getFriendInvitationCode()));
  }

  @Override
  public ResponseEntity<ResultMessageResponseDto> inputInvitationCodeAndGetReward(User user, String code){
    log.info("""

        userId={},
        step=친구초대코드_입력_시작,
        status=SUCCESS

        """, user.getId());
    userService.checkFriendInvitationCodeAndProvideReward(user, code);
    log.info("""

        userId={},
        step=친구초대코드_입력_완료,
        status=SUCCESS

        """, user.getId());

    return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(
        ErrorCode.SUCCESS.getCode(), "친구와 본인 모두 보상을 획득하였습니다."));
  }

  @Override
  public ResponseEntity<ResultMessageResponseDto> updateDormantAccount(User user){

    log.info("""

        userId={},
        step=휴면계정_상태변경_시작,
        status=SUCCESS

        """, user.getId());
    userService.updateDormantAccount(user);
    log.info("""

        userId={},
        step=휴면계정_상태변경_완료,
        status=SUCCESS

        """, user.getId());

    return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(
        ErrorCode.SUCCESS.getCode(), "휴면 계정 정보를 업데이트 하였습니다."));
  }

  @Override
  public ResponseEntity<ResultMessageResponseDto> saveUserAccessLog(User user){

    log.info("""

        userId={},
        step=유저_접근로그_저장_시작,
        status=SUCCESS

        """, user.getId());
    userService.saveUserAccessLog(user);
    log.info("""

        userId={},
        step=유저_접근로그_저장_완료,
        status=SUCCESS

        """, user.getId());

    return ResponseEntity.status(HttpStatus.OK)
        .body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "유저 접속 정보가 저장되었습니다."));
  }

  @Override
  public ResponseEntity<ResultMessageResponseDto> saveMembership(SaveMembershipRequestDto dto, User user) {
    log.info("""

        userId={},
        step=유저_구독_시작,
        status=SUCCESS

        """, user.getId());
    userService.saveMembership(dto.duration(), user);
    log.info("""

        userId={},
        step=유저_구독_완료,
        status=SUCCESS

        """, user.getId());

    return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "멤버십이 구독되었습니다."));
  }


}
