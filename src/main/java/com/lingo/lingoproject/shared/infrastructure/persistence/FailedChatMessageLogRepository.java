package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.shared.domain.model.FailedChatMessageLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FailedChatMessageLogRepository extends JpaRepository<FailedChatMessageLog, Long> {

}
