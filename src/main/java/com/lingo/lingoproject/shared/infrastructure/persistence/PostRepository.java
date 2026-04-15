package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.shared.domain.model.Post;
import com.lingo.lingoproject.shared.domain.model.Recommendation;
import com.lingo.lingoproject.shared.domain.model.PostTopic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

  Page<Post> findByRecommendation(Recommendation recommendation, Pageable pageable);

  @Modifying
  @Query("update Post p set p.likeCount = p.likeCount + 1 where p.id = :postId")
  void increasePostLikeCount(@Param("postId") Long postId);

  @Modifying
  @Query("update Post p set p.likeCount = p.likeCount - 1 where p.id = :postId")
  void decreasePostLikeCount(@Param("postId") Long postId);

  Page<Post> findByRecommendationAndTopic(Recommendation recommendation, PostTopic topic, Pageable pageable);
}
