package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.UserAccessLog;
import com.lingo.lingoproject.domain.enums.Gender;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccessLogRepository extends JpaRepository<UserAccessLog, Long> {

  int countByCreateAtAndGender(LocalDateTime createAt, Gender gender);

  boolean existsByUserIdAndCreateAtAfter(Long userId, LocalDateTime createAtAfter);

  long countByCreateAtAfter(LocalDateTime createAtAfter);

  List<UserAccessLog> findAllByCreateAtAfter(LocalDateTime createAtAfter);

  UserAccessLog findFirstByUserIdOrderByCreateAtDesc(Long userId);
}
