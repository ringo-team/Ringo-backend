package com.lingo.lingoproject.db.repository;

import com.lingo.lingoproject.db.domain.FailedChatMessageLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FailedChatMessageLogRepository extends JpaRepository<FailedChatMessageLog, Long> {

}
