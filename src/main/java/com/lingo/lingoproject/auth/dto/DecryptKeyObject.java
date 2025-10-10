package com.lingo.lingoproject.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class DecryptKeyObject {
  private String key;
  private String iv;
  private String hmacKey;
}
