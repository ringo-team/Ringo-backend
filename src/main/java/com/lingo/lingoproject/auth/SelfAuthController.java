package com.lingo.lingoproject.auth;

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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SelfAuthController {

  private final SelfAuthService selfAuthService;

  @GetMapping("self-auth/callback")
  public ResponseEntity<?> selfAuthCallback(@RequestBody GetUserInfoResponseDto dto, HttpSession session)
      throws Exception {
    String decryptedData = selfAuthService.integrityCheckAndDecryptData(dto.encryptedData(), dto.integrityValue(), session);
    selfAuthService.deserializeAndSaveData(decryptedData);
    return ResponseEntity.ok().build();
  }
}
