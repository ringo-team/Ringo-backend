package com.lingo.lingoproject.chat.application;

import java.time.LocalDateTime;

public interface ChatroomSummaryProjection {
  Long getId();
  LastMessage getLastMessage();
  int getUnreadCount();

  interface LastMessage {
    LocalDateTime getCreatedAt();
    String getContent();
  }
}