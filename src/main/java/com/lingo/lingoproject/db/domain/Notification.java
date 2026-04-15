package com.lingo.lingoproject.db.domain;

import com.lingo.lingoproject.db.domain.enums.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;

@Entity
@Table(name = "NOTIFICATIONS")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@DynamicInsert
public class Notification {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "user_id")
  private User user;

  private NotificationType type;

  @Column(length = 100)
  private String title;
  @Column(length = 100)
  private String message;

  @ColumnDefault(value = "false")
  @Builder.Default
  private boolean isRead = false;

  @ColumnDefault(value = "false")
  @Builder.Default
  private boolean isFinished = false;
}
