package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

}
