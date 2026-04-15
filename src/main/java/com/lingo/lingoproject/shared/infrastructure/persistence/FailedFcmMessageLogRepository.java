package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.shared.domain.model.FailedFcmMessageLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FailedFcmMessageLogRepository extends JpaRepository<FailedFcmMessageLog, Long> {

}
