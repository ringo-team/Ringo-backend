package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.ExceptionMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExceptionMessageRepository extends JpaRepository<ExceptionMessage, Long> {

}
