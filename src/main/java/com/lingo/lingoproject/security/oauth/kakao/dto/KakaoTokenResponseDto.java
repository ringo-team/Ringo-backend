package com.lingo.lingoproject.security.oauth.kakao.dto;

public record KakaoTokenResponseDto(
    String tokenType,
    String accessToken,
    Integer expiresIn,
    String refreshTokenExpiresIn
    ) { }
