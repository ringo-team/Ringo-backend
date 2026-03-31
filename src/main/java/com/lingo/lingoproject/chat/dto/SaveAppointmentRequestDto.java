package com.lingo.lingoproject.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record SaveAppointmentRequestDto(
    @Schema(example = "1", description = "약속을 잡으려는 유저의 id")
    Long registerId,

    @Schema(example = "1", description = "채팅방 id")
    Long chatroomId,

    @Schema(example = "00포차", description = "약속 장소")
    String place,

    @Schema(example = "약속 시간", description = "2025-05-26T13:25:25")
    String appointmentTime,

    @Schema(example = "알람 시간", description = "2025-05-28T13:25:25")
    String alertTime,

    @Schema(example = "1", description = "1이면 알림가도록, 아니면 0")
    Integer isAlert
) {

}
