package com.lingo.lingoproject.security.response;

public record LoginResponseDto(Long userId, String accessToken, String refreshToken) {
}
