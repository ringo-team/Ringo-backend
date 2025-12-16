package com.lingo.lingoproject.domain;

import com.lingo.lingoproject.domain.enums.MatchingStatus;
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
            columnList = "request_user, requested_user, matchingStatus"
        ),
        @Index(
            name = "idx_matchings_request_user",
            columnList = "request_user"
        ),
        @Index(
            name = "idx_matchings_requested_user",
            columnList = "requested_user"
        )
    }
)
public class Matching {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "request_user")
  private User requestUser;

  @ManyToOne
  @JoinColumn(name =  "requested_user")
  private User requestedUser;

  private float matchingScore;
  private String matchingRequestMessage;

  @CreationTimestamp
  @Column(updatable = false, nullable = false)
  private LocalDateTime matchingDate;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private MatchingStatus matchingStatus;
}
