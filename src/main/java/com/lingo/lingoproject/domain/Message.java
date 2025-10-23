package com.lingo.lingoproject.domain;

import java.time.LocalDateTime;
import java.util.List;
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
public class Message  {
  @Id
  private String id;

  private Long chatroomId;

  private Long senderId;

  private List<Long> readerIds;

  private String content;

  private LocalDateTime createdAt;
}
