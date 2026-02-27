package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.Post;
import com.lingo.lingoproject.domain.PostImageMapping;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostImageMappingRepository extends JpaRepository<PostImageMapping, Long> {

  PostImageMapping findByPost(Post post);

  void deleteByPost(Post post);
}
