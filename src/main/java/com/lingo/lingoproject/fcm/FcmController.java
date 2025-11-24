package com.lingo.lingoproject.fcm;

import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.fcm.dto.SaveFcmTokenRequestDto;
import com.lingo.lingoproject.utils.ResultMessageResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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
    fcmService.refreshFcmToken(dto);
    return ResponseEntity.ok().body(new ResultMessageResponseDto("토큰이 정상적으로 저장되었습니다."));
  }
}
