package com.lingo.lingoproject.db.repository;

import com.lingo.lingoproject.db.domain.Post;
import com.lingo.lingoproject.db.domain.PostLikeUserMapping;
import com.lingo.lingoproject.db.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeUserMappingRepository extends JpaRepository<PostLikeUserMapping, Long> {

  boolean existsByPostAndUser(Post post, User user);

  PostLikeUserMapping findByPostAndUser(Post post, User user);
}
