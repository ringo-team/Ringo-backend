package com.lingo.lingoproject.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

@Builder
public record GetChatroomResponseDto(
    @Schema(description = "채팅방 id", example = "5")
    Long chatroomId,
    @Schema(description = "채팅방 크기", example = "2")
    Integer chatroomSize,
    @Schema(description = "채팅방 참여자 닉네임 리스트", example = "불타는 망고")
    String chatOpponent,
    @Schema(description = "채팅 상대방 프로필 url")
    String chatOpponentProfileUrl,
    @Schema(description = "읽지 않은 메세지 개수", example = "5")
    Integer NumberOfNotReadMessages,
    @Schema(description = "마지막 채팅 메세지", example = "안녕하세요")
    String lastChatMessage,
    @Schema(description = "마지막 메세지 시기", example = "2025-11-12 09:28")
    String lastSendDateTime

) {

}
