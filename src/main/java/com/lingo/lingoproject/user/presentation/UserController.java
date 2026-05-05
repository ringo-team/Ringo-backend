package com.lingo.lingoproject.user.presentation;

import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import com.lingo.lingoproject.shared.security.dto.LoginResponseDto;
import com.lingo.lingoproject.shared.security.dto.RegenerateTokenResponseDto;
import com.lingo.lingoproject.shared.utils.ResultMessageResponseDto;
import com.lingo.lingoproject.user.application.AuthTokenUseCase;
import com.lingo.lingoproject.user.application.BlockedFriendUseCase;
import com.lingo.lingoproject.user.application.DormantAccountUseCase;
import com.lingo.lingoproject.user.application.MembershipUseCase;
import com.lingo.lingoproject.user.application.SignupUseCase;
import com.lingo.lingoproject.user.application.UserDeleteUseCase;
import com.lingo.lingoproject.user.application.UserPointUseCase;
import com.lingo.lingoproject.user.application.UserQueryUseCase;
import com.lingo.lingoproject.user.application.UserUpdateUseCase;
import com.lingo.lingoproject.user.presentation.dto.BlockFriendRequestDto;
import com.lingo.lingoproject.user.presentation.dto.GetFriendInvitationCodeResponseDto;
import com.lingo.lingoproject.user.presentation.dto.GetUserInfoResponseDto;
import com.lingo.lingoproject.user.presentation.dto.GetUserLoginIdResponseDto;
import com.lingo.lingoproject.user.presentation.dto.GetUserPointResponseDto;
import com.lingo.lingoproject.user.presentation.dto.LoginInfoDto;
import com.lingo.lingoproject.user.presentation.dto.ResetPasswordRequestDto;
import com.lingo.lingoproject.user.presentation.dto.SaveMembershipRequestDto;
import com.lingo.lingoproject.user.presentation.dto.SignupInfoDto;
import com.lingo.lingoproject.user.presentation.dto.SignupResponseDto;
import com.lingo.lingoproject.user.presentation.dto.SignupUserInfoDto;
import com.lingo.lingoproject.user.presentation.dto.UpdateUserInfoRequestDto;
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
public class UserController implements UserApi {

  private final AuthTokenUseCase authTokenUseCase;
  private final SignupUseCase signupUseCase;
  private final UserQueryUseCase userQueryUseCase;
  private final UserUpdateUseCase userUpdateUseCase;
  private final UserDeleteUseCase userDeleteUseCase;
  private final DormantAccountUseCase dormantAccountUseCase;
  private final MembershipUseCase membershipUseCase;
  private final BlockedFriendUseCase blockedFriendUseCase;
  private final UserPointUseCase userPointUseCase;

  public ResponseEntity<LoginResponseDto> login(
      LoginInfoDto loginInfoDto,
      HttpServletRequest request,
      User user
  ) {
    log.info("step=로그인_시작, status=SUCCESS");
    LoginResponseDto response = authTokenUseCase.login(user);
    log.info("step=로그인_완료, status=SUCCESS");

    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  public ResponseEntity<RegenerateTokenResponseDto> refresh(String refreshToken) {
    log.info("step=토큰_재발급_시작, status=SUCCESS");
    RegenerateTokenResponseDto response = authTokenUseCase.regenerateToken(refreshToken);
    log.info("step=토큰_재발급_완료, status=SUCCESS");

    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  public ResponseEntity<SignupResponseDto> signup(SignupInfoDto dto) {
    log.info("step=회원가입_시작, status=SUCCESS");
    User user = signupUseCase.signup(dto);
    log.info("step=회원가입_완료, status=SUCCESS");

    return ResponseEntity.status(HttpStatus.OK)
        .body(new SignupResponseDto(user.getId(), "회원가입이 완료되었습니다."));
  }

  public ResponseEntity<ResultMessageResponseDto> signupUserInfo(@Valid @RequestBody SignupUserInfoDto dto) {
    log.info("step=회원_정보_저장_시작, status=SUCCESS");
    signupUseCase.saveUserInfo(dto);
    log.info("step=회원_정보_저장_완료, status=SUCCESS");

    return ResponseEntity.status(HttpStatus.OK)
        .body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "유저 정보 저장이 완료되었습니다."));
  }

  public ResponseEntity<ResultMessageResponseDto> verifyDuplicatedLoginId(String loginId) {
    log.info("step=회원_아이디_중복확인_시작, status=SUCCESS");
    boolean isDuplicated = signupUseCase.verifyDuplicatedLoginId(loginId);
    log.info("step=회원_아이디_중복확인_완료, status=SUCCESS");

    if (isDuplicated) {
      return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
          .body(new ResultMessageResponseDto(ErrorCode.PROFILE_DUPLICATED.getCode(), "중복된 아이디입니다."));
    }
    return ResponseEntity.status(HttpStatus.OK)
        .body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "사용가능한 아이디 입니다."));
  }

  public ResponseEntity<ResultMessageResponseDto> verifyDuplicatedNickname(String nickname) {
    log.info("step=회원_닉네임_중복확인_시작, status=SUCCESS");
    boolean isDuplicated = signupUseCase.verifyDuplicatedNickname(nickname);
    log.info("step=회원_닉네임_중복확인_완료, status=SUCCESS");

    if (isDuplicated) {
      return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
          .body(new ResultMessageResponseDto(ErrorCode.PROFILE_DUPLICATED.getCode(), "중복된 닉네임입니다."));
    }
    return ResponseEntity.status(HttpStatus.OK)
        .body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "사용가능한 닉네임입니다."));
  }

  public ResponseEntity<ResultMessageResponseDto> logout(HttpServletRequest request) {
    authTokenUseCase.logout(request);

    return ResponseEntity.status(HttpStatus.OK)
        .body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "로그아웃이 완료되었습니다."));
  }

  @Override
  public ResponseEntity<ResultMessageResponseDto> deleteUser(
      Long userId,
      String reason,
      String feedback,
      User user
  ) {
    if (!user.getId().equals(userId)) {
      throw new RingoException("유저를 탈퇴할 권한이 없습니다.", ErrorCode.NO_AUTH);
    }
    log.info("userId={}, step=회원탈퇴_시작, status=SUCCESS", userId);
    userDeleteUseCase.deleteUser(user, reason, feedback);
    log.info("userId={}, step=회원탈퇴_완료, status=SUCCESS", userId);

    return ResponseEntity.status(HttpStatus.OK)
        .body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "유저를 성공적으로 삭제하였습니다."));
  }

  @Override
  public ResponseEntity<GetUserLoginIdResponseDto> findUserLoginId() {
    String loginId = userQueryUseCase.findUserLoginId();

    return ResponseEntity.status(HttpStatus.OK).body(new GetUserLoginIdResponseDto(loginId));
  }

  @Override
  public ResponseEntity<ResultMessageResponseDto> resetPassword(ResetPasswordRequestDto dto, User user) {
    log.info("userId={}, step=비밀번호_재설정_시작, status=SUCCESS", user.getId());
    userUpdateUseCase.resetPassword(dto.password(), user);
    log.info("userId={}, step=비밀번호_재설정_완료, status=SUCCESS", user.getId());

    return ResponseEntity.status(HttpStatus.OK)
        .body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "password를 성공적으로 변경하였습니다."));
  }

  @Override
  public ResponseEntity<GetUserInfoResponseDto> getUserInfo(Long userId, User user) {
    log.info("userId={}, step=유저정보_조회_시작, status=SUCCESS", user.getId());
    GetUserInfoResponseDto dto = userQueryUseCase.getUserInfo(userId, user);
    log.info("userId={}, step=유저정보_조회_완료, status=SUCCESS", user.getId());

    return ResponseEntity.status(HttpStatus.OK).body(dto);
  }

  @Override
  public ResponseEntity<ResultMessageResponseDto> updateUserInfo(UpdateUserInfoRequestDto dto, User user) {
    log.info("userId={}, step=유저정보_수정_시작, status=SUCCESS", user.getId());
    userUpdateUseCase.updateUserInfo(user, dto);
    log.info("userId={}, step=유저정보_수정_완료, status=SUCCESS", user.getId());

    return ResponseEntity.status(HttpStatus.OK)
        .body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "정상적으로 수정되었습니다."));
  }

  @Override
  public ResponseEntity<GetFriendInvitationCodeResponseDto> getInvitationCode(User user) {
    log.info("userId={}, step=친구초대코드_조회_시작, status=SUCCESS", user.getId());
    return ResponseEntity.status(HttpStatus.OK)
        .body(new GetFriendInvitationCodeResponseDto(user.getFriendInvitationCode()));
  }

  @Override
  public ResponseEntity<ResultMessageResponseDto> inputInvitationCodeAndGetReward(User user, String code) {
    log.info("userId={}, step=친구초대코드_입력_시작, status=SUCCESS", user.getId());
    signupUseCase.checkFriendInvitationCodeAndProvideReward(user, code);
    log.info("userId={}, step=친구초대코드_입력_완료, status=SUCCESS", user.getId());

    return ResponseEntity.status(HttpStatus.OK)
        .body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "친구와 본인 모두 보상을 획득하였습니다."));
  }

  @Override
  public ResponseEntity<ResultMessageResponseDto> updateDormantAccount(User user) {
    log.info("userId={}, step=휴면계정_상태변경_시작, status=SUCCESS", user.getId());
    dormantAccountUseCase.updateDormantAccount(user);
    log.info("userId={}, step=휴면계정_상태변경_완료, status=SUCCESS", user.getId());

    return ResponseEntity.status(HttpStatus.OK)
        .body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "휴면 계정 정보를 업데이트 하였습니다."));
  }

  @Override
  public ResponseEntity<ResultMessageResponseDto> saveUserAccessLog(User user) {
    log.info("userId={}, step=유저_접근로그_저장_시작, status=SUCCESS", user.getId());
    dormantAccountUseCase.saveUserAccessLog(user);
    log.info("userId={}, step=유저_접근로그_저장_완료, status=SUCCESS", user.getId());

    return ResponseEntity.status(HttpStatus.OK)
        .body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "유저 접속 정보가 저장되었습니다."));
  }

  @Override
  public ResponseEntity<ResultMessageResponseDto> saveBlockedFriend(User user, BlockFriendRequestDto dto) {
    blockedFriendUseCase.blockFriend(user, dto.phoneNumbers());
    return ResponseEntity.status(HttpStatus.OK)
        .body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "친구 차단이 성공적으로 이루어졌습니다."));
  }

  @Override
  public ResponseEntity<GetUserPointResponseDto> getUserPoints(User user) {
    GetUserPointResponseDto response = userPointUseCase.getUserPoints(user);
    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  @Override
  public ResponseEntity<ResultMessageResponseDto> saveMembership(SaveMembershipRequestDto dto, User user) {
    log.info("userId={}, step=유저_구독_시작, status=SUCCESS", user.getId());
    membershipUseCase.saveMembership(dto.duration(), user);
    log.info("userId={}, step=유저_구독_완료, status=SUCCESS", user.getId());

    return ResponseEntity.status(HttpStatus.OK)
        .body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "멤버십이 구독되었습니다."));
  }
}
