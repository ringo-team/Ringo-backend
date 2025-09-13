package com.lingo.lingoproject.domain;

import com.lingo.lingoproject.domain.enums.MatchingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
public class Matching {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "request_user")
  private UserEntity requestUser;

  @ManyToOne
  @JoinColumn(name =  "requested_user")
  private UserEntity requestedUser;

  @CreationTimestamp
  @Column(updatable = false, nullable = false)
  private LocalDateTime matchingDate;

  @Column(nullable = false)
  private MatchingStatus matchingStatus;
}
