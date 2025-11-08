package com.lingo.lingoproject.survey.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record UploadSurveyRequestDto(

    @Schema(description = "설문지 응답", example = "5")
    Integer answer,

    @Schema(description = "설문번호", example = "12")
    Integer surveyNum
) {

}
