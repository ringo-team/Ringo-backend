package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.Notification;
import com.lingo.lingoproject.domain.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

  List<Notification> findAllByUser(User user);
}
