package com.lingo.lingoproject.shared.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@EntityListeners(AuditingEntityListener.class)
public class UserActivityLog {

  public static UserActivityLog of(User user, LocalDateTime start, LocalDateTime end, int activityMinuteDuration) {
    return UserActivityLog.builder()
        .user(user)
        .start(start)
        .end(end)
        .activityMinuteDuration(activityMinuteDuration)
        .build();
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "user_id")
  private User user;

  @Column(updatable = false)
  private int activityMinuteDuration;

  @Column(updatable = false)
  private LocalDateTime start;
  @Column(updatable = false)
  private LocalDateTime end;

  @CreationTimestamp
  @Column(updatable = false)
  private LocalDateTime createAt;

}
