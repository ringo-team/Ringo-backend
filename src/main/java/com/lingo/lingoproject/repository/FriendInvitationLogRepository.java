package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.FriendInvitationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FriendInvitationLogRepository extends JpaRepository<FriendInvitationLog, Long> {
  @Query("select count(l) from FriendInvitationLog l where l.friendId = :userId and l.isRealCode = :isRealCode")
  int getNumberOfParticipation(Long userId, boolean isRealCode);
}
