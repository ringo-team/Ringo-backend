package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.User;

import com.lingo.lingoproject.domain.enums.Gender;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByEmail(String email);

  List<User> findAllByEmailIn(Collection<String> emails);

  Optional<User> findByFriendInvitationCode(String friendInvitationCode);

  List<User> findAllByEmailIsContaining(String email);

  List<User> findAllByCreatedAtBetween(LocalDateTime createdAtAfter, LocalDateTime createdAtBefore);

  List<User> findAllByIdIn(Collection<Long> ids);
}
