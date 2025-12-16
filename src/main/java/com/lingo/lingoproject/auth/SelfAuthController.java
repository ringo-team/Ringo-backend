package com.lingo.lingoproject.auth;


import com.lingo.lingoproject.exception.ErrorCode;
import com.lingo.lingoproject.exception.RingoException;
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
public class SelfAuthController {

  private final SelfAuthService selfAuthService;

  @GetMapping("self-auth/callback")
  public ResponseEntity<?> selfAuthCallback(
      @RequestParam(value = "token_version_id") String tokenVersionId,
      @RequestParam(value = "enc_data") String encryptedData,
      @RequestParam(value = "integrity_value") String integrityValue
  ) {
    try {

      log.info("step=유저_정보_복호화_시작, status=SUCCESS");
      String decryptedData = selfAuthService.validateIntegrityAndDecryptData(tokenVersionId, encryptedData, integrityValue);
      log.info("step=유저_정보_복호화_완료, status=SUCCESS");

      log.info("step=유저_정보_역직렬화_저장, status=SUCCESS");
      selfAuthService.deserializeAndSaveUserInfo(decryptedData);
      log.info("step=유저_정보_역직렬화_완료, status=SUCCESS");

    }catch (Exception e){
      log.error("step=유저_정보_역직렬화_실패, status=FAILED", e);
      throw new RingoException("본인인증 api 콜백 데이터를 복호화하거나 정보를 저장하는데 오류가 발생하였습니다.",
          ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return ResponseEntity.ok().build();
  }
}
