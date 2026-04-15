package com.lingo.lingoproject.shared.domain.event;

public interface DomainEventPublisher {
  void publish(DomainEvent event);
}
