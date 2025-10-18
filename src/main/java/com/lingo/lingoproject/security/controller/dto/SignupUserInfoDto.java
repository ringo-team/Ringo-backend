package com.lingo.lingoproject.security.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SignupUserInfoDto(

    @Schema(description = "유저 id", example = "5")
    @NotBlank
    Long id,

    @Schema(description = "닉네임", example = "불타는 망고")
    @NotBlank
    String nickname,

    @NotNull
    Address address,

    @NotNull
    Address activeAddress,

    @Schema(description = "직업", example = "개발자")
    @NotBlank
    String job,

    @Schema(description = "키", example = "177")
    @NotBlank
    String height,

    @Schema(description = "흡연여부", example = "NEVER", allowableValues = {"SMOKING", "ELECTRONIC", "NO_SMOKING", "NEVER"})
    @NotBlank
    String isSmoking,

    @Schema(description = "음주여부", example = "OFTEN", allowableValues = {"ALWAYS", "OFTEN", "RARELY", "ON_NEED", "NEVER"})
    @NotBlank
    String isDrinking,

    @Schema(description = "종교", example = "CHRISTIANITY",
        allowableValues = {"CHRISTIANITY", "BUDDHISM", "CATHOLIC", "ATHEIST", "ETC"})
    @NotBlank
    String religion,

    @Schema(description = "소개문구", example = "안녕하세요")
    String biography,

    @Schema(description = "해시태그", example = "[\"운동\", \"건강\"]")
    @Size(max = 5)
    List<String> hashtags
) {

}
