package com.lingo.lingoproject.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CreateChatroomResponseDto(
    @Schema(description = "응답 결과", example = "0000")
    String result,
    @Schema(description = "채팅방 id", example = "14")
    Long chatroomId
) {

}
