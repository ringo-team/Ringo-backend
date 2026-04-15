package com.lingo.lingoproject.db.repository;

import com.lingo.lingoproject.db.domain.FcmToken;
import com.lingo.lingoproject.db.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

  Optional<FcmToken> findByUser(User user);

  void deleteByUser(User user);
}
