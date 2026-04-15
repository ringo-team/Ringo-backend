package com.lingo.lingoproject.shared.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "FAILED_CHAT_MESSAGE_LOG")
public class FailedChatMessageLog {

  public static FailedChatMessageLog of(Long roomId, Exception e, String messageId, String destination, String userLoginId) {
    return FailedChatMessageLog.builder()
        .roomId(roomId)
        .errorMessage(e.getMessage())
        .errorCause(e.getCause() != null ? e.getCause().getMessage() : null)
        .messageId(messageId)
        .destination(destination)
        .userLoginId(userLoginId)
        .build();
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private String messageId;

  @Lob
  private String errorMessage;
  private String errorCause;

  private Long roomId;
  private String destination;

  @Column(length = 20)
  private String userLoginId;
}
