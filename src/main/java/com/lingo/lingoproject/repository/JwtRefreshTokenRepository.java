package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.JwtRefreshToken;
import com.lingo.lingoproject.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JwtRefreshTokenRepository extends JpaRepository<JwtRefreshToken,Long> {

  JwtRefreshToken findByUser(User user);

  JwtRefreshToken deleteAllByUser(User user);
}
