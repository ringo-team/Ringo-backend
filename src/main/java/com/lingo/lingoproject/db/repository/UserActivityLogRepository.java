package com.lingo.lingoproject.db.repository;

import com.lingo.lingoproject.db.domain.UserActivityLog;
import java.time.LocalDateTime;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserActivityLogRepository extends JpaRepository<UserActivityLog, Long> {

  Collection<UserActivityLog> findAllByStartAfter(LocalDateTime startAfter);
}
