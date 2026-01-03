package com.lingo.lingoproject.domain;

import com.lingo.lingoproject.retry.RedisQueueMessagePayLoad;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Table(name = "FAILED_FCM_MESSAGE_LOG")
@EntityListeners(AuditingEntityListener.class)
public class FailedFcmMessageLog extends RedisQueueMessagePayLoad {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String token;
  private String errorMessage;
  private String errorCause;
  private String title;

  @CreationTimestamp
  @Column(updatable = false)
  private LocalDateTime createAt;

  public static FailedFcmMessageLog of(Exception exception, String token, String title, String message){
    return FailedFcmMessageLog.builder()
        .token(token)
        .errorMessage(exception.getMessage())
        .errorCause(exception.getCause() != null ? exception.getCause().getMessage() : null)
        .title(title)
        .message(message)
        .retryCount(0)
        .build();
  }

}
