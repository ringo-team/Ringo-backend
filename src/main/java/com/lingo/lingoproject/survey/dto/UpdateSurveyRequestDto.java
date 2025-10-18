package com.lingo.lingoproject.survey.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateSurveyRequestDto(
    @NotNull(message = "id는 필수값입니다.")
    @Schema(description = "설문 id", example = "5")
    Long surveyId,
    @Schema(description = "목적", example = "SPACE", allowableValues = {"SPACE", "SELF_REPRESENTATION", "CONTENT", "SHARING"})
    String category,
    @Schema(description = "질문", example = "전통적인 인테리어를 좋아한다")
    String content,
    @Schema(description = "질문 목적", example = "전통적 미학을 선호하는 경향을 파악.")
    String purpose
) {

}
