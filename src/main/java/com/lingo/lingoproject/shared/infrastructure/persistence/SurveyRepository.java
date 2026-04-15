package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.shared.domain.model.Survey;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveyRepository extends JpaRepository<Survey, Long> {

  List<Survey> findAllBySurveyNumBetween(Integer surveyNumAfter, Integer surveyNumBefore);

  List<Survey> findAllBySurveyNumIn(Collection<Integer> surveyNums);

  Survey findBySurveyNum(Integer surveyNum);
}
