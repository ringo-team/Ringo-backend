package com.lingo.lingoproject;

import com.lingo.lingoproject.match.dto.MatchScoreResultInterface;
import com.lingo.lingoproject.repository.AnsweredSurveyRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class RingoRepositoryTest {
  @Autowired
  AnsweredSurveyRepository answeredSurveyRepository;

  @Test
  void matchScoreTest(){
    List<MatchScoreResultInterface> scores = answeredSurveyRepository.calcMatchScore(1L, 2L);
    System.out.println("영역별 점수 " + scores.get(0).getCategory() + " : " + scores.get(0).getAvgAnswer());
  }
}
