package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.shared.domain.model.UserAccessLog;
import com.lingo.lingoproject.shared.domain.model.Gender;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccessLogRepository extends JpaRepository<UserAccessLog, Long> {

  int countByCreateAtAndGender(LocalDateTime createAt, Gender gender);

  boolean existsByUserIdAndCreateAtAfter(Long userId, LocalDateTime createAtAfter);

  long countByCreateAtAfter(LocalDateTime createAtAfter);

  List<UserAccessLog> findAllByCreateAtAfter(LocalDateTime createAtAfter);

  UserAccessLog findFirstByUserIdOrderByCreateAtDesc(Long userId);
}
