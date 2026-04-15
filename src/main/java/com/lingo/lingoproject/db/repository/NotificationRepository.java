package com.lingo.lingoproject.db.repository;

import com.lingo.lingoproject.db.domain.Notification;
import com.lingo.lingoproject.db.domain.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

  List<Notification> findAllByUser(User user);
}
