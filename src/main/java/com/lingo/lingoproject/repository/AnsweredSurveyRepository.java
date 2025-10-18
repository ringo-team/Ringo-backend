package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.AnsweredSurvey;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.match.dto.MatchScoreResultInterface;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnsweredSurveyRepository extends JpaRepository<AnsweredSurvey, Long> {

  void deleteAllByUser(User user);

  List<AnsweredSurvey> findAllByUser(User user);

  @Query(value = "select s.category, avg(1 - abs(A.answer-B.answer)/4.0) as avgAnswer "
      + "from surveys s "
      + "join (select * from answered_surveys s1 where s1.user_id = :user1) A "
      + "on s.id = A.survey_id "
      + "join (select * from answered_surveys s2 where s2.user_id = :user2) B "
      + "on B.survey_id = (select id from surveys where s.confront_survey_num = confront_survey_num) "
      + "group by s.category", nativeQuery = true)
  List<MatchScoreResultInterface> calcMatchScore(@Param("user1") Long user1, @Param("user2") Long user2);
}
