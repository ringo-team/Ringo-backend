package com.lingo.lingoproject.api.auth.dto;

import lombok.Builder;

@Builder
public record AuthWindowRequestDto(
    String m,
    String tokenVersionId,
    String encryptedData,
    String integrityValue
) {

}
