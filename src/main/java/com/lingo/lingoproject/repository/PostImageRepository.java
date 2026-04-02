package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.Post;
import com.lingo.lingoproject.domain.PostImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {

  PostImage findByPost(Post post);

  void deleteByPost(Post post);

  List<PostImage> findAllByPost(Post post);
}
