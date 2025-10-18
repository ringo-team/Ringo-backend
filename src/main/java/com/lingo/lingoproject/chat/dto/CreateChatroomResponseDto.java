package com.lingo.lingoproject.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CreateChatroomResponseDto(
    @Schema(description = "채팅방 id", example = "14")
    Long chatroomId
) {

}
