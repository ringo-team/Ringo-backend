package com.lingo.lingoproject.db.repository;

import com.lingo.lingoproject.db.domain.Comment;
import com.lingo.lingoproject.db.domain.Post;
import com.lingo.lingoproject.db.domain.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {

  void deleteAllByPost(Post post);

  List<Comment> findByIdAndUser(Long id, User user);

  boolean existsByIdAndUser(Long id, User user);

  List<Comment> findAllByPost(Post post);

  @Modifying
  @Query("update Comment c set c.likeCount = c.likeCount + 1 where c.id = :commentId")
  void increaseCommentLikeCount(@Param("commentId") Long commentId);

  @Modifying
  @Query("update Comment c set c.likeCount = c.likeCount - 1 where c.id = :commentId")
  void decreaseCommentLikeCount(@Param("commentId") Long commentId);
}
