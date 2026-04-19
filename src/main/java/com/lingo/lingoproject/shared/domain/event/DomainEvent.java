package com.lingo.lingoproject.shared.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 모든 도메인 이벤트의 기반 클래스.
 *
 * <p>각 이벤트는 고유 ID({@code eventId}), 이벤트 타입({@code eventType}),
 * 발생 시각({@code occurredAt})을 공통으로 갖는다.
 * 서브클래스는 생성자에서 {@link DomainEventType}을 반드시 전달해야 한다.</p>
 *
 * <h2>현재 발행되는 이벤트 목록</h2>
 * <ul>
 *   <li>{@link com.lingo.lingoproject.matching.domain.event.MatchingRequestedEvent}: 매칭 요청 시</li>
 *   <li>{@link com.lingo.lingoproject.matching.domain.event.MatchingAcceptedEvent}: 매칭 수락 시</li>
 *   <li>{@link com.lingo.lingoproject.matching.domain.event.MatchingRejectedEvent}: 매칭 거절 시</li>
 *   <li>{@link com.lingo.lingoproject.report.domain.event.UserSuspendedEvent}: 유저 영구 정지 시</li>
 *   <li>{@link com.lingo.lingoproject.community.domain.event.PostCreatedEvent}: 게시물 생성 시</li>
 *   <li>{@link com.lingo.lingoproject.community.domain.event.CommentCreatedEvent}: 댓글 생성 시</li>
 * </ul>
 */
public abstract class DomainEvent {

  private final UUID eventId = UUID.randomUUID();
  private final DomainEventType eventType;
  private final LocalDateTime occurredAt = LocalDateTime.now();

  protected DomainEvent(DomainEventType eventType) {
    this.eventType = eventType;
  }

  public UUID getEventId()             { return eventId; }
  public DomainEventType getEventType() { return eventType; }
  public LocalDateTime getOccurredAt() { return occurredAt; }
}
