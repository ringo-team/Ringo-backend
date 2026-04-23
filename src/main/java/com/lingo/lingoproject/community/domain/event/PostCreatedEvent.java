package com.lingo.lingoproject.community.domain.event;

import com.lingo.lingoproject.shared.domain.event.DomainEvent;
import com.lingo.lingoproject.shared.domain.event.DomainEventType;
import com.lingo.lingoproject.shared.domain.model.Post;
import lombok.Getter;

/**
 * 게시물 생성 시 발행되는 도메인 이벤트.
 *
 * <p>향후 알림 전송, 피드 갱신, 통계 집계 등의
 * 후속 처리를 이벤트 기반으로 확장할 수 있다.</p>
 */
@Getter
public class PostCreatedEvent extends DomainEvent {

  private final Long   postId;
  private final String title;
  private final String content;
  private final String place;

  public PostCreatedEvent(Post post) {
    super(DomainEventType.POST_CREATED);
    this.postId = post.getId();
    this.title = post.getTitle();
    this.content = post.getContent();
    this.place = post.getPlace();
  }

}