package com.lingo.lingoproject.shared.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Table(
    name = "MATCHINGS",
    indexes = {
        @Index(
            name = "idx_matchings_req_reqd_status",
            columnList = "request_user_id, requested_user_id, matchingStatus"
        ),
        @Index(
            name = "idx_matchings_request_user",
            columnList = "request_user_id"
        ),
        @Index(
            name = "idx_matchings_requested_user",
            columnList = "requested_user_id"
        )
    }
)
public class Matching {

  public static Matching of(
      User requestUser,
      User requestedUser,
      float matchingScore,
      MatchingStatus status,
      String message
  ) {
    return Matching.builder()
        .requestUser(requestUser)
        .requestedUser(requestedUser)
        .matchingStatus(status)
        .matchingScore(matchingScore)
        .matchingRequestMessage(message)
        .build();
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "request_user_id")
  private User requestUser;

  @ManyToOne
  @JoinColumn(name =  "requested_user_id")
  private User requestedUser;

  private float matchingScore;

  @Column(length = 250)
  private String matchingRequestMessage;

  @CreationTimestamp
  @Column(updatable = false, nullable = false)
  private LocalDateTime matchingDate;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private MatchingStatus matchingStatus;

  public void accept() {
    this.matchingStatus = MatchingStatus.ACCEPTED;
  }

  public void reject() {
    this.matchingStatus = MatchingStatus.REJECTED;
  }

  public void updateRequestMessage(String message) {
    this.matchingStatus = MatchingStatus.PENDING;
    this.matchingRequestMessage = message;
  }

  public UserMatchingLog createMatchingLog(){
    return UserMatchingLog.of(
        requestUser.getId(),
        id,
        matchingStatus,
        requestedUser.getGender()
    );
  }

  public UserMatchingLog createRespondLog(User respondingUser) {
    return UserMatchingLog.of(respondingUser.getId(), id, matchingStatus, respondingUser.getGender());
  }
}
