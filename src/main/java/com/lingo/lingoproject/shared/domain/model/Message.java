package com.lingo.lingoproject.shared.domain.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


@Document(collection = "messages")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

  public static Message of(Long chatroomId, Long senderId, String content, List<Long> readerIds) {
    return Message.builder()
        .id(UUID.randomUUID().toString())
        .chatroomId(chatroomId)
        .senderId(senderId)
        .content(content)
        .readerIds(readerIds)
        .createdAt(LocalDateTime.now())
        .build();
  }

  @Id
  private String id;

  private Long chatroomId;

  private Long senderId;

  private List<Long> readerIds;

  private String content;

  private LocalDateTime createdAt;
}
