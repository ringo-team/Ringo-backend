package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.UserAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccessLogRepository extends JpaRepository<UserAccessLog, Long> {

}
