package com.lingo.lingoproject.chat.presentation.dto;


import com.lingo.lingoproject.shared.domain.model.Appointment;
import com.lingo.lingoproject.shared.domain.model.Message;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetChatMessageResponseDto {

  @Schema(description = "chat-message-id", example = "1")
  String id;

  @Schema(description = "메세지를 보낸 유저 id", example = "12")
  @NotBlank
  Long senderId;

  @Schema(description = "채팅방 id", example = "5")
  @NotBlank
  Long chatroomId;

  @Schema(description = "채팅방 크기", example = "2")
  Integer chatroomSize;

  @Schema(description = "채팅 메세지", example = "안녕하세요")
  @NotBlank
  String content;

  @Schema(description = "채팅 생성일자", example = "2025-07-05 14:46:37")
  String createdAt;

  @Schema(description = "채팅 읽음 여부", example = "1 이면 읽음, 0이면 읽지 않음")
  Integer isRead;

  @Schema(description = "채팅 읽은 사람 리스트", example = "[\"12\", \"25\"]")
  List<Long> readerIds;

  @NotBlank
  String type;

  LocalDateTime appointmentTime;

  String place;

  public static GetChatMessageResponseDto from(Message m) {
    return GetChatMessageResponseDto.builder()
        .id(m.getId())
        .chatroomId(m.getChatroomId())
        .senderId(m.getSenderId())
        .content(m.getContent())
        .createdAt(m.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
        .readerIds(m.getReaderIds())
        .type("PLAIN")
        .build();
  }

  public static GetChatMessageResponseDto forAppointment(Appointment appointment) {
    return GetChatMessageResponseDto.builder()
        .content("일정이 등록되었어요")
        .appointmentTime(appointment.getAppointmentTime())
        .place(appointment.getPlace())
        .type("APPOINTMENT")
        .build();
  }

  public static GetChatMessageResponseDto forAppointmentAlert(Appointment appointment) {
    return GetChatMessageResponseDto.builder()
        .content("오늘 약속이 예정되어 있어요")
        .appointmentTime(appointment.getAppointmentTime())
        .place(appointment.getPlace())
        .type("APPOINTMENT")
        .build();
  }
}
