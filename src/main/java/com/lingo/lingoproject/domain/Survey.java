package com.lingo.lingoproject.domain;

import com.lingo.lingoproject.domain.enums.SurveyCategory;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "SURVEYS")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class Survey {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Integer surveyNum;
  private Integer confrontSurveyNum;

  @Enumerated(EnumType.STRING)
  private SurveyCategory category;
  private String purpose;
  private String content;

  private String matchedReasonForHigherAnswer;
  private String matchedReasonForLowerAnswer;

  private String categoryForHigherAnswer;
  private String categoryForLowerAnswer;
}
