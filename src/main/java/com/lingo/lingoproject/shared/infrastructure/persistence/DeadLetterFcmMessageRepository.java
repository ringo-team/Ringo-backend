package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.shared.domain.model.DeadLetterFcmMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeadLetterFcmMessageRepository extends JpaRepository<DeadLetterFcmMessage, Long> {

}
