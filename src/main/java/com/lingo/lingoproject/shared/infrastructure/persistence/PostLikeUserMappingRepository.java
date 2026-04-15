package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.shared.domain.model.Post;
import com.lingo.lingoproject.shared.domain.model.PostLikeUserMapping;
import com.lingo.lingoproject.shared.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeUserMappingRepository extends JpaRepository<PostLikeUserMapping, Long> {

  boolean existsByPostAndUser(Post post, User user);

  PostLikeUserMapping findByPostAndUser(Post post, User user);
}
