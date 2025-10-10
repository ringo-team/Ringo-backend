package com.lingo.lingoproject.auth;

import lombok.Builder;

@Builder
public record PopUpCompositionDto(
    String m,
    String tokenVersionId,
    String encryptedData,
    String integrityValue
) {

}
