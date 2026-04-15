package com.lingo.lingoproject.db.repository;

import com.lingo.lingoproject.db.domain.Appointment;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

  List<Appointment> findAllByAlertTimeBeforeAndIsAlert(LocalDateTime alertTimeBefore, boolean isAlert);
}
