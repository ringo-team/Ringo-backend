package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.shared.domain.model.Hashtag;
import com.lingo.lingoproject.shared.domain.model.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HashtagRepository extends JpaRepository<Hashtag, Long> {

  List<Hashtag> findAllByUser(User user);

  List<Hashtag> findAllByUserIn(List<User> users);

  void deleteAllByUser(User user);
}
