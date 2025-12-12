package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.DeadLetterFcmMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeadLetterFcmMessageRepository extends JpaRepository<DeadLetterFcmMessage, Long> {

}
