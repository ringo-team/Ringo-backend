package com.lingo.lingoproject.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateChatroomDto(
    @Schema(description = "유저1 id", example = "12")
    @NotNull
    Long user1Id,
    @Schema(description = "유저2 id", example = "43")
    @NotNull
    Long user2Id,
    @Schema(description = "채팅타입", example = "USER", allowableValues = {"USER", "SNAP", "COMPLAIN"})
    @NotBlank
    String chatType
) {

}
