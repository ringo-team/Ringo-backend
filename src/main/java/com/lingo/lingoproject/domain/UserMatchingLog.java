package com.lingo.lingoproject.domain;

import com.lingo.lingoproject.domain.enums.Gender;
import com.lingo.lingoproject.domain.enums.MatchingStatus;
import com.lingo.lingoproject.utils.Timestamp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "USER_MATCHING_LOGS")
public class UserMatchingLog{
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(updatable = false)
  private Long userId;

  @Column(updatable = false)
  private String username;

  @Enumerated(EnumType.STRING)
  @Column(updatable = false)
  private Gender gender;

  @CreationTimestamp
  @Column(updatable = false)
  private LocalDateTime createdAt;
}
