package com.lingo.lingoproject.db.repository;

import com.lingo.lingoproject.db.domain.Comment;
import com.lingo.lingoproject.db.domain.CommentLikeUserMapping;
import com.lingo.lingoproject.db.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentLikeUserMappingRepository extends JpaRepository<CommentLikeUserMapping, Long> {

  CommentLikeUserMapping findByCommentAndUser(Comment comment, User user);
}
