package com.lingo.lingoproject.chat.dto;

public record SaveAppointmentRequestDto(
    Long registerId,
    Long chatroomId,
    String place,
    String appointmentTime,
    String alertTime,
    Integer isAlert
) {

}
