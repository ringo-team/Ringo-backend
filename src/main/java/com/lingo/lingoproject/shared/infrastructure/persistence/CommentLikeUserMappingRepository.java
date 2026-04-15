package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.shared.domain.model.Comment;
import com.lingo.lingoproject.shared.domain.model.CommentLikeUserMapping;
import com.lingo.lingoproject.shared.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentLikeUserMappingRepository extends JpaRepository<CommentLikeUserMapping, Long> {

  CommentLikeUserMapping findByCommentAndUser(Comment comment, User user);
}
