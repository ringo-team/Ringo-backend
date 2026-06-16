package com.lingo.lingoproject.community.domain.event;

import com.lingo.lingoproject.shared.domain.event.DomainEvent;
import com.lingo.lingoproject.shared.domain.event.DomainEventType;
import com.lingo.lingoproject.shared.domain.model.User;
import lombok.Getter;

/**
 * 댓글 생성 시 발행되는 도메인 이벤트.
 *
 * <p>향후 게시물 작성자에게 알림 전송, 통계 집계 등의
 * 후속 처리를 이벤트 기반으로 확장할 수 있다.</p>
 */
@Getter
public class CommentCreatedEvent extends DomainEvent {

  private final Long commentId;
  private final String content;
  private final Long postId;
  private final User author;
  private final User commenter;

  public CommentCreatedEvent(
      Long commentId,
      String content,
      Long postId,
      User author,
      User commenter
  ) {
    super(DomainEventType.COMMENT_CREATED);
    this.commentId = commentId;
    this.content = content;
    this.postId = postId;
    this.author = author;
    this.commenter = commenter;
  }
}