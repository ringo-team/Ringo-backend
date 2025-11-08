package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.FcmToken;
import com.lingo.lingoproject.domain.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

  Optional<FcmToken> findByUser(User user);

  List<String> findByUserIn(Collection<User> users);

  void deleteByUser(User user);
}
