package com.lingo.lingoproject.user.presentation.dto.oauth.google;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleTokenResponseDto(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("expires_in") String expiresIn,
    @JsonProperty("refresh_token") String refreshToken,
    @JsonProperty("refresh_token_expires_in") String refreshTokenExpiresIn,
    @JsonProperty("scope") String scope,
    @JsonProperty("token_type") String tokenType
) { }
