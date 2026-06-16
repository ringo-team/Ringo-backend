package com.lingo.lingoproject.matching.domain.event;

import com.lingo.lingoproject.shared.domain.event.DomainEvent;
import com.lingo.lingoproject.shared.domain.event.DomainEventType;
import com.lingo.lingoproject.shared.domain.model.User;
import lombok.Getter;

@Getter
public class UserScrapEvent extends DomainEvent {

  private final User user;
  private final User scrappedUser;

  public UserScrapEvent(User user, User scrappedUser) {
    super(DomainEventType.USER_SCRAP);
    this.user = user;
    this.scrappedUser = scrappedUser;
  }
}
