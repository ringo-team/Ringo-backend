package com.lingo.lingoproject.fcm;

import com.lingo.lingoproject.fcm.dto.RefreshFcmTokenRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FcmController {

  private final FcmService fcmService;

  @PostMapping("/fcm/refresh")
  public ResponseEntity<String> refreshFcmToken(@RequestBody RefreshFcmTokenRequestDto dto){
    fcmService.refreshFcmToken(dto);
    return ResponseEntity.ok().body("토큰이 정상적으로 저장되었습니다.");
  }
}
