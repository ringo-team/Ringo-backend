package com.lingo.lingoproject.matching.domain.service;

import com.lingo.lingoproject.matching.presentation.dto.MatchScoreResultInterface;
import com.lingo.lingoproject.shared.infrastructure.persistence.AnsweredSurveyRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 두 유저 간 매칭 점수를 계산하는 Domain Service.
 * IO(DB 조회)를 포함하지만 순수 점수 계산 로직을 캡슐화한다.
 */
@Service
@RequiredArgsConstructor
public class MatchScoreCalculator {

  private final AnsweredSurveyRepository answeredSurveyRepository;

  @Value("${ringo.config.survey.space_weight}")
  private float SURVEY_SPACE_WEIGHT;

  @Value("${ringo.config.survey.self_representation_weight}")
  private float SURVEY_SELF_REPRESENTATION_WEIGHT;

  @Value("${ringo.config.survey.content_weight}")
  private float SURVEY_CONTENT_WEIGHT;

  @Value("${ringo.config.survey.sharing_weight}")
  private float SURVEY_SHARING_WEIGHT;

  public float calculate(Long user1Id, Long user2Id) {
    List<MatchScoreResultInterface> list = answeredSurveyRepository.calcMatchScore(user1Id, user2Id);
    float score = 0;
    for (MatchScoreResultInterface result : list) {
      switch (result.getCategory()) {
        case "SPACE"             -> score += result.getAvgAnswer() * SURVEY_SPACE_WEIGHT;
        case "SELF_REPRESENTATION" -> score += result.getAvgAnswer() * SURVEY_SELF_REPRESENTATION_WEIGHT;
        case "SHARING"           -> score += result.getAvgAnswer() * SURVEY_SHARING_WEIGHT;
        case "CONTENT"           -> score += result.getAvgAnswer() * SURVEY_CONTENT_WEIGHT;
      }
    }
    return score;
  }
}
