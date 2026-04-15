package com.lingo.lingoproject.user.presentation.dto.auth;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CryptoTokenInfo {

  public static CryptoTokenInfo of(String siteCode, String tokenVersionId, String token) {
    return CryptoTokenInfo.builder()
        .siteCode(siteCode)
        .tokenVersionId(tokenVersionId)
        .token(token)
        .build();
  }

  private String siteCode;
  private String tokenVersionId;
  private String token;
}
