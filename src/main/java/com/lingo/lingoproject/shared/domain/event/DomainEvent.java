package com.lingo.lingoproject.shared.domain.event;

import java.time.LocalDateTime;

public abstract class DomainEvent {
  private final LocalDateTime occurredAt = LocalDateTime.now();

  public LocalDateTime getOccurredAt() {
    return occurredAt;
  }
}
