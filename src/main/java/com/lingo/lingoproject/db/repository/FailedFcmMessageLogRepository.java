package com.lingo.lingoproject.db.repository;

import com.lingo.lingoproject.db.domain.FailedFcmMessageLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FailedFcmMessageLogRepository extends JpaRepository<FailedFcmMessageLog, Long> {

}
