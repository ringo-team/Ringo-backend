package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.shared.domain.model.Comment;
import com.lingo.lingoproject.shared.domain.model.CommentLikeUserMapping;
import com.lingo.lingoproject.shared.domain.model.User;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentLikeUserMappingRepository extends JpaRepository<CommentLikeUserMapping, Long> {

  CommentLikeUserMapping findByCommentAndUser(Comment comment, User user);

  boolean existsByCommentAndUser(Comment comment, User user);

  void deleteByCommentAndUser(Comment comment, User user);

  void deleteAllByComment(Comment comment);

  void deleteAllByCommentIn(Collection<Comment> comments);
}
