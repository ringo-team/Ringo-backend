package com.lingo.lingoproject.chat.dto;

import lombok.Builder;

@Builder
public record GetChatroomResponseDto(
    Long chatroomId,
    String chatroomName,
    Integer NumOfNonReadMessages,
    String lastChatMessage
) {

}
