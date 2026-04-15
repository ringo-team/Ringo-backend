package com.lingo.lingoproject.db.repository;

import com.lingo.lingoproject.db.domain.Hashtag;
import com.lingo.lingoproject.db.domain.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HashtagRepository extends JpaRepository<Hashtag, Long> {

  List<Hashtag> findAllByUser(User user);

  void deleteAllByUser(User user);
}
