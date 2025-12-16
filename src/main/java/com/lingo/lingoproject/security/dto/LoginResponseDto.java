package com.lingo.lingoproject.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 및 토큰 재발급 결과")
public record LoginResponseDto(
    @Schema(description = "응답 결과", example = "0000") String result,
    @Schema(description = "사용자 식별자", example = "101") Long userId,
    @Schema(description = "액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...") String accessToken,
    @Schema(description = "리프레시 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...") String refreshToken) {
}
