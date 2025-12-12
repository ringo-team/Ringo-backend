package com.lingo.lingoproject.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class DeadLetterFcmMessage {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String token;
  private String errorMessage;
  private String errorCause;
  private String message;
  private String userEmail;
  private Integer retryCount;
  private String title;

  @CreationTimestamp
  @Column(updatable = false)
  private LocalDateTime createAt;

  public static DeadLetterFcmMessage from(FailedFcmMessageLog log){
    return DeadLetterFcmMessage.builder()
        .id(log.getId())
        .token(log.getToken())
        .errorMessage(log.getErrorMessage())
        .errorCause(log.getErrorCause())
        .message(log.getMessage())
        .userEmail(log.getUserEmail())
        .retryCount(log.getRetryCount())
        .title(log.getTitle())
        .build();
  }
}
