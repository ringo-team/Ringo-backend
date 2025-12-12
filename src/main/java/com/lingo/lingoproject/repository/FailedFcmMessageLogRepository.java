package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.FailedFcmMessageLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FailedFcmMessageLogRepository extends JpaRepository<FailedFcmMessageLog, Long> {

}
