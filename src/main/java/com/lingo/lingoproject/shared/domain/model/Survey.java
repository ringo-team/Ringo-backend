package com.lingo.lingoproject.shared.domain.model;

import com.lingo.lingoproject.shared.domain.model.SurveyCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "SURVEYS",
    indexes = {
        @Index(
            name = "idx_surveys_survey_num",
            columnList = "surveyNum"
        )
    }
)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class Survey {

  public static Survey of(
      int surveyNum,
      int confrontSurveyNum,
      SurveyCategory category,
      String content,
      String purpose,
      String positiveKeyword,
      String negativeKeyword
  ) {
    return Survey.builder()
        .surveyNum(surveyNum)
        .confrontSurveyNum(confrontSurveyNum)
        .category(category)
        .content(content)
        .purpose(purpose)
        .keywordForHigherAnswer(positiveKeyword)
        .keywordForLowerAnswer(negativeKeyword)
        .build();
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Integer surveyNum;
  private Integer confrontSurveyNum;

  @Enumerated(EnumType.STRING)
  private SurveyCategory category;
  @Column(length = 100)
  private String purpose;
  @Column(length = 100)
  private String content;

  @Column(length = 100)
  private String matchedReasonForHigherAnswer;
  @Column(length = 100)
  private String matchedReasonForLowerAnswer;

  private String keywordForHigherAnswer;
  private String keywordForLowerAnswer;
}
