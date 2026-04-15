package com.lingo.lingoproject.api.user.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record UpdateUserInfoRequestDto(

    @Schema(description = "활동지")
    Address activeAddress,

    @Schema(description = "직업", example = "개발자")
    String job,

    @Schema(description = "키", example = "177")
    String height,

    @Schema(description = "학력", example = "대학교졸업")
    String degree,

    @Schema(description = "음주여부", example = "OFTEN", allowableValues = {"ALWAYS", "OFTEN", "RARELY", "ON_NEED", "NEVER"})
    String isDrinking,

    @Schema(description = "흡연여부", example = "NEVER", allowableValues = {"SMOKING", "ELECTRONIC", "NO_SMOKING", "NEVER"})
    String isSmoking,

    @Schema(description = "종교", example = "CHRISTIANITY",
        allowableValues = {"CHRISTIANITY", "BUDDHISM", "CATHOLIC", "ATHEIST", "ETC"})
    String religion,

    @Schema(description = "mbti", example = "ENFP")
    String mbti,

    @Schema(description = "해시태그", example = "[\"온라인게임\", \"축구\"]")
    List<String> hashtag,

    @Schema(description = "자기 소개", example = "안녕하세요")
    String biography,

    @Schema(description = "닉네임", example = "불타는 망고")
    String nickname
){}