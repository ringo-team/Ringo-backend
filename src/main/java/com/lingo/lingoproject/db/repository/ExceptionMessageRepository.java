package com.lingo.lingoproject.db.repository;

import com.lingo.lingoproject.db.domain.ExceptionMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExceptionMessageRepository extends JpaRepository<ExceptionMessage, Long> {

}
