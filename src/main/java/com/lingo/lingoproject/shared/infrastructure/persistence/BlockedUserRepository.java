package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.shared.domain.model.BlockedUser;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BlockedUserRepository extends JpaRepository<BlockedUser, Long> {

  boolean existsByBlockedUserId(Long blockedUserId);

  boolean existsByPhoneNumber(String phoneNumber);

  @Query("SELECT b.blockedUserId FROM BlockedUser b")
  List<Long> findAllBlockedUserIds();
}
