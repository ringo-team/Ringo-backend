package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.Hashtag;
import com.lingo.lingoproject.domain.User;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HashtagRepository extends JpaRepository<Hashtag, Long> {
  List<Hashtag> findAllByUserIn(Collection<User> users);

  List<Hashtag> findAllByUser(User user);

  void deleteAllByUser(User user);
}
