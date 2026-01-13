package com.lingo.lingoproject.user.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record UpdateUserInfoRequestDto(
    @Schema(description = "키", example = "177")
    String height,

    @Schema(description = "흡연여부", example = "NEVER", allowableValues = {"SMOKING", "ELECTRONIC", "NO_SMOKING", "NEVER"})
    String isSmoking,

    @Schema(description = "음주여부", example = "OFTEN", allowableValues = {"ALWAYS", "OFTEN", "RARELY", "ON_NEED", "NEVER"})
    String isDrinking,

    @Schema(description = "직업", example = "개발자")
    String job,

    @Schema(description = "종교", example = "CHRISTIANITY",
        allowableValues = {"CHRISTIANITY", "BUDDHISM", "CATHOLIC", "ATHEIST", "ETC"})
    String religion,

    @Schema(description = "자기 소개", example = "안녕하세요")
    String biography,

    @Schema(description = "mbti", example = "ENFP")
    String mbti
){}