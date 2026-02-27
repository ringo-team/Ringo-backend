package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.Post;
import com.lingo.lingoproject.domain.Recommendation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {

  Page<Post> findByRecommendation(Recommendation recommendation, Pageable pageable);
}
