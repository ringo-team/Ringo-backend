package com.lingo.lingoproject.community.domain.service;

import com.lingo.lingoproject.shared.domain.model.Comment;
import com.lingo.lingoproject.shared.domain.model.Place;
import com.lingo.lingoproject.shared.domain.model.Post;
import com.lingo.lingoproject.shared.domain.model.SubComment;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * 커뮤니티 도메인의 비즈니스 규칙을 담당하는 Domain Service.
 *
 * <p>게시물·댓글·대댓글의 소유권 검증 규칙을 캡슐화한다.
 * 인프라 의존 없이 순수한 도메인 불변 조건만 다룬다.</p>
 *
 * <h2>소유권 규칙</h2>
 * <ul>
 *   <li>게시물 수정·삭제: 작성자(author)만 가능</li>
 *   <li>댓글 수정·삭제: 댓글 작성자(user)만 가능</li>
 *   <li>대댓글 수정·삭제: 대댓글 작성자(user)만 가능</li>
 * </ul>
 */
@Slf4j
@Service
public class CommunityDomainService {

  /**
   * 게시물 소유권을 검증한다. 게시자 본인만 수정·삭제할 수 있다.
   *
   * @throws RingoException 소유자가 아닌 경우
   */
  public void validatePostOwnership(Post post, User user) {
    if (!post.getAuthor().getId().equals(user.getId())) {
      log.warn("step=게시물_권한없음, postId={}, requestUserId={}, authorId={}",
          post.getId(), user.getId(), post.getAuthor().getId());
      throw new RingoException("게시자만 게시물을 처리할 수 있습니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
    }
  }

  /**
   * 댓글 소유권을 검증한다. 작성자 본인만 수정·삭제할 수 있다.
   *
   * @throws RingoException 소유자가 아닌 경우
   */
  public void validateCommentOwnership(Comment comment, User user) {
    if (!comment.getUser().getId().equals(user.getId())) {
      log.warn("step=댓글_권한없음, commentId={}, requestUserId={}, ownerId={}",
          comment.getId(), user.getId(), comment.getUser().getId());
      throw new RingoException("댓글을 처리할 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
    }
  }
}