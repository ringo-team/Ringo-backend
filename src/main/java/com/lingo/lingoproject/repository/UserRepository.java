package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.UserEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
  Optional<UserEntity> findByEmail(String email);
  UserEntity findByUsername(String username);
}
