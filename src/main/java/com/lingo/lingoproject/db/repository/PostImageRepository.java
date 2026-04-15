package com.lingo.lingoproject.db.repository;

import com.lingo.lingoproject.db.domain.Post;
import com.lingo.lingoproject.db.domain.PostImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {

  PostImage findByPost(Post post);

  void deleteByPost(Post post);

  List<PostImage> findAllByPost(Post post);
}
