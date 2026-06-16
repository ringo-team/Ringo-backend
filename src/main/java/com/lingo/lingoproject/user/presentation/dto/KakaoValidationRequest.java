package com.lingo.lingoproject.user.presentation.dto;

public record KakaoValidationRequest(
    String providerId,
    String email,
    String nickname
) {

}
