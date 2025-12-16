package com.lingo.lingoproject.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record RegenerateTokenResponseDto(
    @Schema(description = "응답 결과", example = "0000") String result,
    @Schema(description = "사용자 식별자", example = "101") Long userId,
    @Schema(description = "액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...") String accessToken,
    @Schema(description = "리프레시 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...") String refreshToken
) {
}
