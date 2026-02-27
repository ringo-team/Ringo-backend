package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.Comment;
import com.lingo.lingoproject.domain.Post;
import com.lingo.lingoproject.domain.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

  void deleteAllByPost(Post post);

  List<Comment> findByIdAndUser(Long id, User user);

  boolean existsByIdAndUser(Long id, User user);

  List<Comment> findAllByPost(Post post);
}
