package com.lingo.lingoproject.user.presentation;
import com.lingo.lingoproject.user.application.SelfAuthUseCase;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "self-auth-controller", description = "유저 핸드폰 본인인증 관련 api")
public class SelfAuthController {

  private final SelfAuthUseCase selfAuthService;

  @GetMapping("self-auth/callback")
  public ResponseEntity<?> selfAuthCallback(
      @RequestParam(value = "token_version_id") String tokenVersionId,
      @RequestParam(value = "enc_data") String encryptedData,
      @RequestParam(value = "integrity_value") String integrityValue
  ) throws Exception{

    log.info("step=본인인증_복호화_시작");
    String decryptedData = selfAuthService.validateIntegrityAndDecryptData(tokenVersionId, encryptedData, integrityValue);
    log.info("step=본인인증_복호화_완료");

    log.info("step=본인인증_정보_역직렬화_시작");
    selfAuthService.deserializeAndSaveUserInfo(decryptedData);
    log.info("step=본인인증_정보_역직렬화_완료");

    return ResponseEntity.status(HttpStatus.OK).build();
  }
}
