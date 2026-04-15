package com.lingo.lingoproject.user.presentation.dto.oauth.kakao;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoTokenResponseDto(
    @JsonProperty("token_type") String tokenType,
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("expires_in") Integer expiresIn,
    @JsonProperty("refresh_token_expires_in") String refreshTokenExpiresIn
    ) { }
