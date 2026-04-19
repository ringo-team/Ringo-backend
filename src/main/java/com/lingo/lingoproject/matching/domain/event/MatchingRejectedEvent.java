package com.lingo.lingoproject.matching.domain.event;

import com.lingo.lingoproject.shared.domain.event.DomainEvent;
import com.lingo.lingoproject.shared.domain.event.DomainEventType;

/**
 * 매칭 거절 시 발행되는 도메인 이벤트.
 *
 * <p>매칭 피요청자가 요청을 거절하면 발행된다.
 * 향후 거절 알림, 통계 집계 등의 후속 처리에 활용할 수 있다.</p>
 */
public class MatchingRejectedEvent extends DomainEvent {

  private final Long matchingId;
  private final Long requestUserId;
  private final Long requestedUserId;

  public MatchingRejectedEvent(Long matchingId, Long requestUserId, Long requestedUserId) {
    super(DomainEventType.MATCHING_REJECTED);
    this.matchingId      = matchingId;
    this.requestUserId   = requestUserId;
    this.requestedUserId = requestedUserId;
  }

  public Long getMatchingId()      { return matchingId; }
  public Long getRequestUserId()   { return requestUserId; }
  public Long getRequestedUserId() { return requestedUserId; }
}