package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.JwtRefreshToken;
import com.lingo.lingoproject.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JwtTokenRepository extends JpaRepository<JwtRefreshToken,Long> {

  JwtRefreshToken findByUser(UserEntity user);
}
