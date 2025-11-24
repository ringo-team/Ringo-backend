package com.lingo.lingoproject.survey.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record UpdateSurveyRequestDto(

    @Schema(description = "목적", example = "SPACE", allowableValues = {"SPACE", "SELF_REPRESENTATION", "CONTENT", "SHARING"})
    @NotBlank
    String category,

    @Schema(description = "질문", example = "전통적인 인테리어를 좋아한다")
    @NotBlank
    String content,

    @Schema(description = "질문 목적", example = "전통적 미학을 선호하는 경향을 파악.")
    @NotBlank
    String purpose
) {

}
