package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.Comment;
import com.lingo.lingoproject.domain.CommentLikeUserMapping;
import com.lingo.lingoproject.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentLikeUserMappingRepository extends JpaRepository<CommentLikeUserMapping, Long> {

  CommentLikeUserMapping findByCommentAndUser(Comment comment, User user);
}
