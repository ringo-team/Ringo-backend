package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.shared.domain.model.Post;
import com.lingo.lingoproject.shared.domain.model.PostCategory;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

  //Page<Post> findByRecommendation(Place recommendation, Pageable pageable);

  @Modifying
  @Query("update Post p set p.likeCount = p.likeCount + 1 where p.id = :postId")
  void increasePostLikeCount(@Param("postId") Long postId);

  @Modifying
  @Query("update Post p set p.likeCount = p.likeCount - 1 where p.id = :postId")
  void decreasePostLikeCount(@Param("postId") Long postId);

  @Modifying
  @Query("update Post p set p.commentCount = p.commentCount + 1 where p.id = :postId")
  void increaseCommentCount(Long postId);

  @Modifying
  @Query("update Post p set p.commentCount = p.commentCount - :count where p.id = :postId")
  void decreaseCommentCount(Long postId, int count);

  Page<Post> findByCategory(PostCategory category, Pageable pageable);

  List<Post> findAllByIdIn(Collection<Long> ids);

  //Page<Post> findByRecommendationAndTopic(Place recommendation, PostCategory topic, Pageable pageable);
}
