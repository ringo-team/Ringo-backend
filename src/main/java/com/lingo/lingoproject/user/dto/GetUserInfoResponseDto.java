package com.lingo.lingoproject.user.dto;

import com.lingo.lingoproject.domain.enums.Drinking;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record GetUserInfoResponseDto(

    @Schema(description = "유저 id", example = "4")
    Long id,

    @Schema(description = "성별", example = "MALE", allowableValues = {"MALE", "FEMALE"})
    String gender,

    @Schema(description = "닉네임", example = "불타는 망고")
    String nickname,

    @Schema(description = "키", example = "177")
    String height,

    @Schema(description = "음주여부", example = "OFTEN", allowableValues = {"ALWAYS", "OFTEN", "RARELY", "ON_NEED", "NEVER"})
    String isDrinking,

    @Schema(description = "흡연여부", example = "NEVER", allowableValues = {"SMOKING", "ELECTRONIC", "NO_SMOKING", "NEVER"})
    String isSmoking,

    @Schema(description = "직업", example = "개발자")
    String job,

    @Schema(description = "종교", example = "CHRISTIANITY",
        allowableValues = {"CHRISTIANITY", "BUDDHISM", "CATHOLIC", "ATHEIST", "ETC"})
    String religion,

    @Schema(description = "소개문구", example = "안녕하세요")
    String biography
) {

}
