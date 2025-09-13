package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.JwtToken;
import com.lingo.lingoproject.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JwtTokenRepository extends JpaRepository<JwtToken,Long> {

  JwtToken findByUser(UserEntity user);
}
