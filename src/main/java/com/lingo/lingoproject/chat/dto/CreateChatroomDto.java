package com.lingo.lingoproject.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CreateChatroomDto(
    @Schema(description = "유저1 id", example = "12")
    Long user1Id,
    @Schema(description = "유저2 id", example = "43")
    Long user2Id,
    @Schema(description = "채팅타입", example = "USER", allowableValues = {"USER", "SNAP", "COMPLAIN"})
    String chatType
) {

}
