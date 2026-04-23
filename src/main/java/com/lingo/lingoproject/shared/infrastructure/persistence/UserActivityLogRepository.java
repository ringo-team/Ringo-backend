package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.domain.model.UserActivityLog;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserActivityLogRepository extends JpaRepository<UserActivityLog, Long> {

  Collection<UserActivityLog> findAllByStartAfter(LocalDateTime startAfter);

  UserActivityLog findFirstByUserOrderByCreateAtDesc(User user);

  List<UserActivityLog> findByUserAndCreateAtAfter(User user, LocalDateTime createAtAfter);
}
