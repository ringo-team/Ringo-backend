package com.lingo.lingoproject.community.domain.event;

import com.lingo.lingoproject.shared.domain.event.DomainEvent;
import com.lingo.lingoproject.shared.domain.event.DomainEventType;

/**
 * 게시물 생성 시 발행되는 도메인 이벤트.
 *
 * <p>향후 알림 전송, 피드 갱신, 통계 집계 등의
 * 후속 처리를 이벤트 기반으로 확장할 수 있다.</p>
 */
public class PostCreatedEvent extends DomainEvent {

  private final Long   postId;
  private final Long   authorId;
  private final String topic;

  public PostCreatedEvent(Long postId, Long authorId, String topic) {
    super(DomainEventType.POST_CREATED);
    this.postId   = postId;
    this.authorId = authorId;
    this.topic    = topic;
  }

  public Long   getPostId()   { return postId; }
  public Long   getAuthorId() { return authorId; }
  public String getTopic()    { return topic; }
}