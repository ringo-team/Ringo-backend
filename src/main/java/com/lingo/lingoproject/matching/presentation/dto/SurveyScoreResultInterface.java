package com.lingo.lingoproject.matching.presentation.dto;

import com.lingo.lingoproject.shared.domain.model.SurveyCategory;

public interface SurveyScoreResultInterface {
  Long getUserId();
  SurveyCategory getCategory();
  Float getAvgAnswer();
}