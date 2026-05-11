package com.lingo.lingoproject.user.application;

public record KakaoValidationRequest(
    String providerId,
    String email,
    String nickname
) {

}
