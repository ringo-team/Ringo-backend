package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.domain.model.UserProfileClickHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileClickHistoryRepository extends JpaRepository<UserProfileClickHistory, Long> {

  boolean existsByUserAndProfileUser(User user, User profileUser);
}
