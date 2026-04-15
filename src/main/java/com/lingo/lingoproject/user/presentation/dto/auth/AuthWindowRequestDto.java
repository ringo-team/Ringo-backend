package com.lingo.lingoproject.user.presentation.dto.auth;

import lombok.Builder;

@Builder
public record AuthWindowRequestDto(
    String m,
    String tokenVersionId,
    String encryptedData,
    String integrityValue
) {

  public static AuthWindowRequestDto of(String m, String tokenVersionId, String encryptedData, String integrityValue) {
    return AuthWindowRequestDto.builder()
        .m(m)
        .tokenVersionId(tokenVersionId)
        .encryptedData(encryptedData)
        .integrityValue(integrityValue)
        .build();
  }
}
