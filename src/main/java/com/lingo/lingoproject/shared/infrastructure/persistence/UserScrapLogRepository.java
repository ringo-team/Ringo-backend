package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.domain.model.UserScrapLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserScrapLogRepository extends JpaRepository<UserScrapLog, Long> {

  boolean existsByUserAndScrappedUser(User user, User scrappedUser);
}
