package com.lingo.lingoproject.chat.application;

import java.time.Instant;
import java.time.LocalDateTime;

public interface ChatroomSummaryProjection {
  Long getChatroomId();
  String getContent();
  LocalDateTime getCreatedAt();
  int getUnreadCount();

}