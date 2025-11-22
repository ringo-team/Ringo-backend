package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.FailedMessageLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FailedMessageLogRepository extends JpaRepository<FailedMessageLog, Long> {

}
