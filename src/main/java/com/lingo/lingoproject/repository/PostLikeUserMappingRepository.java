package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.Post;
import com.lingo.lingoproject.domain.PostLikeUserMapping;
import com.lingo.lingoproject.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeUserMappingRepository extends JpaRepository<PostLikeUserMapping, Long> {

  boolean existsByPostAndUser(Post post, User user);

  PostLikeUserMapping findByPostAndUser(Post post, User user);
}
