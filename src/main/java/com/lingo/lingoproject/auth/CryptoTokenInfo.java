package com.lingo.lingoproject.auth;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CryptoTokenInfo {
  private String siteCode;
  private String tokenVersionId;
  private String token;
}
