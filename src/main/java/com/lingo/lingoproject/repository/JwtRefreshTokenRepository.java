package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.JwtRefreshToken;
import com.lingo.lingoproject.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JwtRefreshTokenRepository extends JpaRepository<JwtRefreshToken,Long> {

  Optional<JwtRefreshToken> findByUser(User user);

  void deleteByUser(User user);

  boolean existsByUser(User user);
}
