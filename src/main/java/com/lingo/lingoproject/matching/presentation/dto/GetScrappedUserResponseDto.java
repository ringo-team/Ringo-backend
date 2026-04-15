package com.lingo.lingoproject.matching.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record GetScrappedUserResponseDto(

    @Schema(description = "유저 Id", example = "1")
    Long userId,

    @Schema(description = "유저 닉네임", example = "망고")
    String nickname,

    @Schema(description = "유저 나이", example = "24")
    Integer age,

    @Schema(description = "프로필 url", example = "http://aws.....")
    String profileUrl,

    @Schema(description = "얼굴 인증 여부, 인증 1, 미인증 0")
    Integer ifFaceVerified
) {

}
