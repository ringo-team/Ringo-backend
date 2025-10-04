package com.lingo.lingoproject.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GetUserInfoResponseDto(
    @JsonProperty("enc_data") String encryptedData,
    @JsonProperty("integrity_value") String integrityValue
) {

}
