package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.User;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByEmail(String email);

  List<User> findAllByEmailIn(Collection<String> emails);

  Optional<User> findByFriendInvitationCode(String friendInvitationCode);

  List<User> findAllByIdIn(Collection<Long> ids);

  boolean existsByEmail(String email);

  List<User> findAllByCreatedAtAfter(LocalDateTime createdAtAfter);
}
