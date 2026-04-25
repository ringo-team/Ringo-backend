package com.lingo.lingoproject.survey.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record UploadSurveyRequestDto(

    @Schema(description = "설문지 응답, 매우 그렇다: 5 그 이후 하나씩 내려감", example = "5")
    Integer answer,

    @Schema(description = "설문번호", example = "12")
    Integer surveyNum
) {

}
