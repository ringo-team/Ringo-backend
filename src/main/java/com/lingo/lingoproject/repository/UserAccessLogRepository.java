package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.UserAccessLog;
import com.lingo.lingoproject.domain.enums.Gender;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccessLogRepository extends JpaRepository<UserAccessLog, Long> {

  long countByCreateAt(LocalDateTime createAt);

  int countByCreateAtAndGender(LocalDateTime createAt, Gender gender);

  List<UserAccessLog> findAllByCreateAtBetween(LocalDateTime createAtAfter, LocalDateTime createAtBefore);

  long countByCreateAtBetween(LocalDateTime createAtAfter, LocalDateTime createAtBefore);
}
