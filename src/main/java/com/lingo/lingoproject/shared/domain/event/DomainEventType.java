package com.lingo.lingoproject.shared.domain.event;

/**
 * 시스템에서 발행되는 모든 도메인 이벤트 타입을 한 곳에서 정의한다.
 *
 * <p>새로운 이벤트 추가 시 이 enum에 타입을 먼저 등록한 뒤
 * 해당 이벤트 클래스를 생성한다.</p>
 */
public enum DomainEventType {

  // ── Matching Context ──────────────────────────────
  MATCHING_REQUESTED,
  MATCHING_ACCEPTED,
  MATCHING_REJECTED,

  // ── Report Context ────────────────────────────────
  USER_SUSPENDED,

  // ── Community Context ─────────────────────────────
  POST_CREATED,
  COMMENT_CREATED
}
