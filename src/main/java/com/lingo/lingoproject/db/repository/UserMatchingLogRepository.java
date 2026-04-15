package com.lingo.lingoproject.db.repository;

import com.lingo.lingoproject.db.domain.UserMatchingLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserMatchingLogRepository extends JpaRepository<UserMatchingLog, Long> {

}
