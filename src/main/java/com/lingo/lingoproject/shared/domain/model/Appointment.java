package com.lingo.lingoproject.shared.domain.model;

import com.lingo.lingoproject.chat.presentation.dto.SaveAppointmentRequestDto;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
@Table(name = "APPOINTMENTS")
public class Appointment {

  public static Appointment of(Chatroom chatroom, User register, SaveAppointmentRequestDto dto) {

    LocalDateTime appointmentTime = LocalDateTime.parse(dto.appointmentTime());
    LocalDateTime alertTime = Optional.ofNullable(dto.alertTime()).map(LocalDateTime::parse).orElse(null);
    boolean isAlert = dto.isAlert() == 1;

    if (isAlert && alertTime == null) throw new RingoException("[약속잡기] 잘못된 요청", ErrorCode.BAD_REQUEST);

    return Appointment.builder()
        .chatroom(chatroom)
        .register(register)
        .place(dto.place())
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
