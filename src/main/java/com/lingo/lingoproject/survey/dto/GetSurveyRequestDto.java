package com.lingo.lingoproject.survey.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record GetSurveyRequestDto(
    @Schema(description = "설문 번호", example = "3")
    Integer surveyNum,
    @Schema(description = "대치 질문 번호", example = "5")
    Integer confrontSurveyNum,
    @Schema(description = "설문 카테고리", example = "SPACE")
    String category,
    @Schema(description = "질문 내용", example = "전통적인 인테리어를 좋아한다")
    String content,
    @Schema(description = "질문 목적", example = "전통적 미학을 선호하는 경향을 파악")
    String purpose
) {

}
