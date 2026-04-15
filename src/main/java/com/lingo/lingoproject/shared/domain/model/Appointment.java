package com.lingo.lingoproject.shared.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
@Table(name = "APPOINTMENTS")
public class Appointment {

  public static Appointment of(Chatroom chatroom, User register, String place,
      LocalDateTime appointmentTime, LocalDateTime alertTime, boolean isAlert) {
    return Appointment.builder()
        .chatroom(chatroom)
        .register(register)
        .place(place)
        .appointmentTime(appointmentTime)
        .alertTime(alertTime)
        .isAlert(isAlert)
        .build();
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "chatroom_id")
  private Chatroom chatroom;

  @ManyToOne
  @JoinColumn(name = "register_id")
  private User register;

  private String place;

  private LocalDateTime appointmentTime;
  private LocalDateTime alertTime;

  @Builder.Default
  private boolean isAlert = false;
}
