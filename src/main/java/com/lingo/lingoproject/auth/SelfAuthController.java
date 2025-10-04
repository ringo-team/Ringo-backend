package com.lingo.lingoproject.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SelfAuthController {

  private final SelfAuthService selfAuthService;

  @GetMapping("self-auth/callback")
  public ResponseEntity<?> selfAuthCallback(@RequestParam(value = "token_version_id") String tokenVersionId,
      @RequestParam(value = "enc_data") String encryptedData,
      @RequestParam(value = "integrity_value") String integrityValue)
      throws Exception {
    String decryptedData = selfAuthService.integrityCheckAndDecryptData(tokenVersionId, encryptedData, integrityValue);
    selfAuthService.deserializeAndSaveData(decryptedData);
    return ResponseEntity.ok().build();
  }
}
