package com.lingo.lingoproject.fcm;

import com.lingo.lingoproject.fcm.dto.SaveFcmTokenRequestDto;
import com.lingo.lingoproject.utils.ResultMessageResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FcmController {

  private final FcmService fcmService;

  @PostMapping("/fcm/refresh")
  public ResponseEntity<ResultMessageResponseDto> refreshFcmToken(@RequestBody SaveFcmTokenRequestDto dto){
    fcmService.refreshFcmToken(dto);
    return ResponseEntity.ok().body(new ResultMessageResponseDto("토큰이 정상적으로 저장되었습니다."));
  }

  @PostMapping("/fcm")
  public ResponseEntity<ResultMessageResponseDto> saveFcmToken(@RequestBody SaveFcmTokenRequestDto dto){
    fcmService.saveFcmToken(dto);
    return ResponseEntity.status(HttpStatus.CREATED).body(new ResultMessageResponseDto("토큰이 정상적으로 저장되었습니다."));
  }
}
