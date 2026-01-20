package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.Survey;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveyRepository extends JpaRepository<Survey, Long> {

  List<Survey> findAllBySurveyNumBetween(Integer surveyNumAfter, Integer surveyNumBefore);

  List<Survey> findAllBySurveyNumIn(Collection<Integer> surveyNums);

  Survey findBySurveyNum(Integer surveyNum);
}
