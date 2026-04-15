package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.shared.domain.model.FriendInvitationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FriendInvitationLogRepository extends JpaRepository<FriendInvitationLog, Long> {
  @Query("select count(l) from FriendInvitationLog l where l.friendId = :userId and l.isRealCode = :isRealCode")
  int getNumberOfParticipation(Long userId, boolean isRealCode);
}
