package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.shared.domain.model.AnsweredSurvey;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.matching.presentation.dto.SurveyScoreResultInterface;
import com.lingo.lingoproject.matching.presentation.dto.MatchedSurveyAnswerInterface;
import com.lingo.lingoproject.matching.presentation.dto.RelatedSurveyAnswerPairInterface;
import com.lingo.lingoproject.survey.presentation.dto.GetUserSurveyResponseDto;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnsweredSurveyRepository extends JpaRepository<AnsweredSurvey, Long> {

  void deleteAllByUser(User user);

  List<AnsweredSurvey> findAllByUser(User user);

  @Query(value = """ 
        select
            S.category as category,
            avg(1 - abs(A.answer-B.answer)/4.0) as avgAnswer
        from Survey S, AnsweredSurvey A, AnsweredSurvey B
        where S.surveyNum = A.surveyNum
          and S.confrontSurveyNum = B.surveyNum
          and A.user.id = :user1
          and B.user.id = :user2
        group by S.category
        """)
  List<SurveyScoreResultInterface> calcSurveyScore(@Param("user1") Long user1, @Param("user2") Long user2);

  @Query(value = """
      select
          B.user.id as userId,
          S.category as category,
          avg(1 - abs(A.answer - B.answer) / 4.0) as avgAnswer
      from Survey S, AnsweredSurvey A, AnsweredSurvey B
      where S.surveyNum = A.surveyNum
        and S.confrontSurveyNum = B.surveyNum
        and A.user.id = :userId
        and B.user.id in :selectedUserIds
      group by B.user.id, S.category
      """)
  List<SurveyScoreResultInterface> batchCalcSurveyScore(
      @Param("userId") Long userId,
      @Param("selectedUserIds") List<Long> selectedUserIds
  );

  @Query(value = """
    select S.id as surveyId, A.answer as answer
    from Survey S, AnsweredSurvey A, AnsweredSurvey B
    where S.surveyNum = A.surveyNum
      and S.confrontSurveyNum = B.surveyNum
      and A.user.id = :user1
      and B.user.id = :user2
      and A.answer = B.answer
  """)
  List<MatchedSurveyAnswerInterface> getMatchedSurveyNum(@Param("user1") Long user1, @Param("user2") Long user2);

  @Query(value = """
        select S.id as surveyId, A.answer as answer, B.answer as confrontAnswer
        from Survey S, AnsweredSurvey A, AnsweredSurvey B
        where S.surveyNum = A.surveyNum
          and S.confrontSurveyNum = B.surveyNum
          and A.user.id = :user1
          and B.user.id = :user2
  """)
  List<RelatedSurveyAnswerPairInterface> getRelatedSurveyAnswerPairs(Long user1, Long user2);

  long countByUser(User user);

  @Query(value = """
      select
          new com.lingo.lingoproject.survey.presentation.dto.GetUserSurveyResponseDto
          (s.surveyNum, s.content, a.answer, :userId, s.content)
      from Survey s
      join AnsweredSurvey a on s.surveyNum = a.surveyNum
      where a.user.id = :userId
      """)
  List<GetUserSurveyResponseDto> getUserSurveyResponseDto(Long userId);


  boolean existsByUserAndUpdatedAtAfter(User user, LocalDateTime updatedAtAfter);

  List<AnsweredSurvey> findAllByUserAndUpdatedAtAfter(User user, LocalDateTime updatedAtAfter);

  List<AnsweredSurvey> findAllByUserIdNotInAndAnswerAndSurveyNumIn(Collection<Long> users, Integer answer, Collection<Integer> surveyNums);

  boolean existsByUserAndSurveyNum(User user, Integer surveyNum);

  AnsweredSurvey findByUserAndSurveyNum(User user, Integer surveyNum);
}
