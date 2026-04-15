package com.lingo.lingoproject.db.repository;

import com.lingo.lingoproject.db.domain.DeadLetterFcmMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeadLetterFcmMessageRepository extends JpaRepository<DeadLetterFcmMessage, Long> {

}
