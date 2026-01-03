package com.lingo.lingoproject.domain;

import com.lingo.lingoproject.domain.enums.Gender;
import com.lingo.lingoproject.domain.enums.MatchingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "USER_MATCHING_LOGS")
public class UserMatchingLog{
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(updatable = false)
  private Long userId;

  @Column(updatable = false)
  private Long matchingId;

  @Column(updatable = false)
  @Enumerated(value = EnumType.STRING)
  private MatchingStatus status;

  @Enumerated(EnumType.STRING)
  @Column(updatable = false)
  private Gender gender;

  @CreationTimestamp
  @Column(updatable = false)
  private LocalDateTime createdAt;
}
