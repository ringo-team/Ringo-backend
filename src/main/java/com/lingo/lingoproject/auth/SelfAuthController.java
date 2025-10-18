package com.lingo.lingoproject.auth;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SelfAuthController {

  private final SelfAuthService selfAuthService;

  @GetMapping("self-auth/callback")
  public ResponseEntity<?> selfAuthCallback(
      @RequestParam(value = "token_version_id") String tokenVersionId,
      @RequestParam(value = "enc_data") String encryptedData,
      @RequestParam(value = "integrity_value") String integrityValue
  ) throws Exception {
    String decryptedData = selfAuthService.integrityCheckAndDecryptData(tokenVersionId, encryptedData, integrityValue);
    selfAuthService.deserializeAndSaveData(decryptedData);
    return ResponseEntity.ok().build();
  }
}
