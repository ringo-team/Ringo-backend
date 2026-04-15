package com.lingo.lingoproject.db.repository;

import com.lingo.lingoproject.db.domain.NotificationOptionOutUser;
import com.lingo.lingoproject.db.domain.User;
import com.lingo.lingoproject.db.domain.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationOptionOutUserRepository extends JpaRepository<NotificationOptionOutUser, Long> {

  boolean existsByUserAndType(User user, NotificationType type);

  void deleteAllByUserAndType(User user, NotificationType type);
}
