package com.lingo.lingoproject.db.repository;

import com.lingo.lingoproject.db.domain.BlockedUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlockedUserRepository extends JpaRepository<BlockedUser, Long> {

  boolean existsByBlockedUserId(Long blockedUserId);

  boolean existsByPhoneNumber(String phoneNumber);
}
