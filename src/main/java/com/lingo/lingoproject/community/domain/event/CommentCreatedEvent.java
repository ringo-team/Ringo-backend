package com.lingo.lingoproject.community.domain.event;

import com.lingo.lingoproject.shared.domain.event.DomainEvent;
import com.lingo.lingoproject.shared.domain.event.DomainEventType;

/**
 * 댓글 생성 시 발행되는 도메인 이벤트.
 *
 * <p>향후 게시물 작성자에게 알림 전송, 통계 집계 등의
 * 후속 처리를 이벤트 기반으로 확장할 수 있다.</p>
 */
public class CommentCreatedEvent extends DomainEvent {

  private final Long commentId;
  private final Long postId;
  private final Long authorId;

  public CommentCreatedEvent(Long commentId, Long postId, Long authorId) {
    super(DomainEventType.COMMENT_CREATED);
    this.commentId = commentId;
    this.postId    = postId;
    this.authorId  = authorId;
  }

  public Long getCommentId() { return commentId; }
  public Long getPostId()    { return postId; }
  public Long getAuthorId()  { return authorId; }
}