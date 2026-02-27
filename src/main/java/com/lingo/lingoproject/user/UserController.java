package com.lingo.lingoproject.user;

import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.exception.ErrorCode;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.user.dto.GetFriendInvitationCodeResponseDto;
import com.lingo.lingoproject.user.dto.GetUserInfoResponseDto;
import com.lingo.lingoproject.user.dto.GetUserLoginIdResponseDto;
import com.lingo.lingoproject.user.dto.ResetPasswordRequestDto;
import com.lingo.lingoproject.user.dto.SaveMembershipRequestDto;
import com.lingo.lingoproject.user.dto.UpdateUserInfoRequestDto;
import com.lingo.lingoproject.utils.ResultMessageResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping()
@Slf4j
@RequiredArgsConstructor
public class UserController implements UserApi{

  private final UserService userService;

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
    log.info("userId={}, step=회원탈퇴_시작, status=SUCCESS", userId);
    userService.deleteUser(user, reason, feedback);
    log.info("userId={}, step=회원탈퇴_완료, status=SUCCESS", userId);

    return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(
        ErrorCode.SUCCESS.getCode(), "유저를 성공적으로 삭제하였습니다."));
  }

  @Override
  public ResponseEntity<GetUserLoginIdResponseDto> findUserLoginId(User user){
    log.info("userId={}, step=유저_ID찾기_시작, status=SUCCESS", user.getId());
    String loginId = userService.findUserLoginId(user);
    log.info("userId={}, step=유저_ID찾기_완료, status=SUCCESS", user.getId());

    return ResponseEntity.status(HttpStatus.OK).body(new GetUserLoginIdResponseDto(loginId));
  }

  @Override
  public ResponseEntity<ResultMessageResponseDto> resetPassword(ResetPasswordRequestDto dto, User user){
    log.info("userId={}, step=비밀번호_재설정_시작, status=SUCCESS", user.getId());
    userService.resetPassword(dto.password(), user);
    log.info("userId={}, step=비밀번호_재설정_완료, status=SUCCESS", user.getId());

    return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(
        ErrorCode.SUCCESS.getCode(), "password를 성공적으로 변경하였습니다."));
  }

  @Override
  public ResponseEntity<GetUserInfoResponseDto> getUserInfo(Long userId, User user){
    log.info("userId={}, step=유저정보_조회_시작, status=SUCCESS", user.getId());
    GetUserInfoResponseDto dto = userService.getUserInfo(userId, user);
    log.info("userId={}, step=유저정보_조회_완료, status=SUCCESS", user.getId());

    return ResponseEntity.status(HttpStatus.OK).body(dto);
  }

  @Override
  public ResponseEntity<ResultMessageResponseDto> updateUserInfo(UpdateUserInfoRequestDto dto, User user){
    log.info("userId={}, step=유저정보_수정_시작, status=SUCCESS", user.getId());
    userService.updateUserInfo(user, dto);
    log.info("userId={}, step=유저정보_수정_완료, status=SUCCESS", user.getId());

    return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(
        ErrorCode.SUCCESS.getCode(), "정상적으로 수정되었습니다."));
  }

  @Override
  public ResponseEntity<GetFriendInvitationCodeResponseDto> getInvitationCode(User user){
    log.info("userId={}, step=친구초대코드_조회_시작, status=SUCCESS", user.getId());
    return ResponseEntity.status(HttpStatus.OK).body(new GetFriendInvitationCodeResponseDto(user.getFriendInvitationCode()));
  }

  @Override
  public ResponseEntity<ResultMessageResponseDto> inputInvitationCodeAndGetReward(User user, String code){
    log.info("userId={}, step=친구초대코드_입력_시작, status=SUCCESS", user.getId());
    userService.checkFriendInvitationCodeAndProvideReward(user, code);
    log.info("userId={}, step=친구초대코드_입력_완료, status=SUCCESS", user.getId());

    return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(
        ErrorCode.SUCCESS.getCode(), "친구와 본인 모두 보상을 획득하였습니다."));
  }

  @Override
  public ResponseEntity<ResultMessageResponseDto> updateDormantAccount(User user){

    log.info("userId={}, step=휴면계정_상태변경_시작, status=SUCCESS", user.getId());
    userService.updateDormantAccount(user);
    log.info("userId={}, step=휴면계정_상태변경_완료, status=SUCCESS", user.getId());

    return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(
        ErrorCode.SUCCESS.getCode(), "휴면 계정 정보를 업데이트 하였습니다."));
  }

  @Override
  public ResponseEntity<ResultMessageResponseDto> saveUserAccessLog(User user){

    log.info("userId={}, step=유저_접근로그_저장_시작, status=SUCCESS", user.getId());
    userService.saveUserAccessLog(user);
    log.info("userId={}, step=유저_접근로그_저장_완료, status=SUCCESS", user.getId());

    return ResponseEntity.status(HttpStatus.OK)
        .body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "유저 접속 정보가 저장되었습니다."));
  }

  @Override
  public ResponseEntity<ResultMessageResponseDto> saveMembership(SaveMembershipRequestDto dto, User user) {
    log.info("userId={}, step=유저_구독_시작, status=SUCCESS", user.getId());
    userService.saveMembership(dto.duration(), user);
    log.info("userId={}, step=유저_구독_완료, status=SUCCESS", user.getId());

    return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "멤버십이 구독되었습니다."));
  }


}
