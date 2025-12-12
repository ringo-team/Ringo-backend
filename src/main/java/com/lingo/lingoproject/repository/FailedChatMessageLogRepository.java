package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.FailedChatMessageLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FailedChatMessageLogRepository extends JpaRepository<FailedChatMessageLog, Long> {

}
