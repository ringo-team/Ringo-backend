package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.shared.domain.model.ExceptionMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExceptionMessageRepository extends JpaRepository<ExceptionMessage, Long> {

}
