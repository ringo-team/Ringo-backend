package com.lingo.lingoproject.matching.domain.event;

import com.lingo.lingoproject.shared.domain.event.DomainEvent;
import com.lingo.lingoproject.shared.domain.event.DomainEventType;
import com.lingo.lingoproject.shared.domain.model.User;
import lombok.Getter;

@Getter
public class UserProfileClickEvent extends DomainEvent {

  private final User profileUser;
  private final User user;

  public UserProfileClickEvent(User profileUser, User user) {
    super(DomainEventType.PROFILE_CLICK);
    this.profileUser = profileUser;
    this.user = user;
  }
}
