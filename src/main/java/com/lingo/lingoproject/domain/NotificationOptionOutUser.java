package com.lingo.lingoproject.domain;

import com.lingo.lingoproject.domain.enums.NotificationType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(
    name = "NOTIFICATION_OPTION_OUT_USER",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "notification_opt_out_user_unique_constraint_user_type",
            columnNames = {"user_id", "type"}
        )
    }
)
public class NotificationOptionOutUser {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "user_id")
  private User user;

  @Enumerated(value = EnumType.STRING)
  private NotificationType type;
}
