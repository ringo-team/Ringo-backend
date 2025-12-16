package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.BlockedUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlockedUserRepository extends JpaRepository<BlockedUser, Long> {

  boolean existsByBlockedUserId(Long blockedUserId);

  boolean existsByPhoneNumber(String phoneNumber);
}
