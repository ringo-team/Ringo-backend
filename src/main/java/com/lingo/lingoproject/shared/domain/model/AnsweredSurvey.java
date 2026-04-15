package com.lingo.lingoproject.shared.domain.model;

import com.lingo.lingoproject.shared.utils.Timestamp;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "ANSWERED_SURVEYS",
    indexes = {
        @Index(
            name = "idx_answered_surveys_user_id_survey_num",
            columnList = "user_id, surveyNum"
        ),
        @Index(
            name = "idx_answered_surveys_user_id_updated_at",
            columnList = "user_id, updatedAt"
        )
    }
)
public class AnsweredSurvey extends Timestamp {

  public static AnsweredSurvey of(User user, int surveyNum, int answer) {
    return AnsweredSurvey.builder()
        .user(user)
        .surveyNum(surveyNum)
        .answer(answer)
        .build();
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "user_id")
  private User user;

  private Integer surveyNum;

  private Integer answer;

}
