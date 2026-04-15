package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.shared.domain.model.BlockedUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlockedUserRepository extends JpaRepository<BlockedUser, Long> {

  boolean existsByBlockedUserId(Long blockedUserId);

  boolean existsByPhoneNumber(String phoneNumber);
}
