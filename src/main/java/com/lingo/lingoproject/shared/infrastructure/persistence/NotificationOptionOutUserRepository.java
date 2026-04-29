package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.shared.domain.model.NotificationOptionOutUser;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.domain.model.NotificationType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationOptionOutUserRepository extends JpaRepository<NotificationOptionOutUser, Long> {

  boolean existsByUserAndType(User user, NotificationType type);

  void deleteAllByUserAndType(User user, NotificationType type);

  List<NotificationOptionOutUser> findAllByUser(User user);
}
