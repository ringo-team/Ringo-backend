package com.lingo.lingoproject.user.presentation.dto.auth;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class DecryptKeyObject {

  public static DecryptKeyObject of(String decryptionKey, String initialValueForDecryption, String hmacKeyForIntegrityCheck) {
    return DecryptKeyObject.builder()
        .decryptionKey(decryptionKey)
        .initialValueForDecryption(initialValueForDecryption)
        .hmacKeyForIntegrityCheck(hmacKeyForIntegrityCheck)
        .build();
  }

  private String decryptionKey;
  private String initialValueForDecryption;
  private String hmacKeyForIntegrityCheck;
}
