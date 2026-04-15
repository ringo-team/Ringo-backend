package com.lingo.lingoproject.matching.domain.event;

import com.lingo.lingoproject.shared.domain.event.DomainEvent;

public class MatchingAcceptedEvent extends DomainEvent {
  private final Long matchingId;
  private final Long requestUserId;
  private final Long requestedUserId;

  public MatchingAcceptedEvent(Long matchingId, Long requestUserId, Long requestedUserId) {
    this.matchingId      = matchingId;
    this.requestUserId   = requestUserId;
    this.requestedUserId = requestedUserId;
  }

  public Long getMatchingId()      { return matchingId; }
  public Long getRequestUserId()   { return requestUserId; }
  public Long getRequestedUserId() { return requestedUserId; }
}
