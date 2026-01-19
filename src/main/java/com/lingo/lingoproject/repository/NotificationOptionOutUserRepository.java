package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.NotificationOptionOutUser;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.domain.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationOptionOutUserRepository extends JpaRepository<NotificationOptionOutUser, Long> {

  boolean existsByUserAndType(User user, NotificationType type);

  void deleteAllByUserAndType(User user, NotificationType type);
}
