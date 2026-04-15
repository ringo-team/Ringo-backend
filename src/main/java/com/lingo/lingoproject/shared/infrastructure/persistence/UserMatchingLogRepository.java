package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.shared.domain.model.UserMatchingLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserMatchingLogRepository extends JpaRepository<UserMatchingLog, Long> {

}
