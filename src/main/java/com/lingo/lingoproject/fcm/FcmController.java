package com.lingo.lingoproject.fcm;

import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.fcm.dto.SaveFcmTokenRequestDto;
import com.lingo.lingoproject.utils.ResultMessageResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class FcmController {

  private final FcmService fcmService;

  @PostMapping("/fcm/refresh")
  public ResponseEntity<ResultMessageResponseDto> refreshFcmToken(
      @RequestBody SaveFcmTokenRequestDto dto,
      @AuthenticationPrincipal User user
  ){
    if(!dto.userId().equals(user.getId())){
      throw new RingoException("토큰을 refresh할 권한이 없습니다.", HttpStatus.FORBIDDEN);
    }

    try {

      log.info("userId={}, step=리프레시_토큰_시작, status=SUCCESS", user.getId());
      fcmService.refreshFcmToken(dto);
      log.info("userId={}, step=리프레시_토큰_완료, status=SUCCESS", user.getId());

      return ResponseEntity.ok().body(new ResultMessageResponseDto("토큰이 정상적으로 저장되었습니다."));

    }catch (Exception e){
      log.error("userId={}, step=리프레시_토큰_실패, status=FAILED", user.getId(), e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("토큰을 리프레시하는데 실패하였습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
    }

  }
}
