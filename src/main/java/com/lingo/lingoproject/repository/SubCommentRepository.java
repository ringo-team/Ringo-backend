package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.Comment;
import com.lingo.lingoproject.domain.SubComment;
import com.lingo.lingoproject.domain.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubCommentRepository extends JpaRepository<SubComment, Long> {

  void deleteAllByComment(Comment comment);

  List<SubComment> findAllByComment(Comment comment);

  boolean existsByIdAndUser(Long id, User user);
}
