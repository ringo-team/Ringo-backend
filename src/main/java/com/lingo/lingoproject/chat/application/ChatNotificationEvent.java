package com.lingo.lingoproject.chat.application;

import com.lingo.lingoproject.shared.domain.event.DomainEvent;
import com.lingo.lingoproject.shared.domain.event.DomainEventType;
import com.lingo.lingoproject.shared.domain.model.User;
import lombok.Getter;

@Getter
public class ChatNotificationEvent extends DomainEvent {
  private User member;
  private User sender;
  private String message;
  private String imageUrl;

  public ChatNotificationEvent(User user, User sender, String message, String imageUrl){
    super(DomainEventType.CHAT);
    this.member = user;
    this.sender = sender;
    this.message = message;
    this.imageUrl = imageUrl;
  }
}
