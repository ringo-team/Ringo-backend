package com.lingo.lingoproject.survey.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class GetUserSurveyResponseDto {

  @Schema(description = "설문 번호", example = "3")
  Integer surveyNum;
  @Schema(description = "설문 질문", example = "전통적인 인테리어를 선호한다.")
  String surveyContent;
  @Schema(description = "설문 응답", example = "4")
  Integer answer;
  @Schema(description = "유저 id", example = "4")
  Long userId;
  @Schema(description = "설문 내용", example = ",,,,")
  String content;

  public GetUserSurveyResponseDto(
      Integer surveyNum,
      String surveyContent,
      Integer answer,
      Long userId,
      String content
  ) {
    this.surveyNum = surveyNum;
    this.surveyContent = surveyContent;
    this.answer = answer;
    this.userId = userId;
    this.content = content;
  }
}
