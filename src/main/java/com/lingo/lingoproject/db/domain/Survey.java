package com.lingo.lingoproject.db.domain;

import com.lingo.lingoproject.db.domain.enums.SurveyCategory;
import jakarta.persistence.Column;
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
