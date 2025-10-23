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
    @Schema(description = "채팅방 참여자 닉네임 리스트", example = "[\"불타는 망고\", \"사자를 사자\"]")
    List<String> participants,
    @Schema(description = "읽지 않은 메세지 개수", example = "5")
    Integer NumberOfNotReadMessages,
    @Schema(description = "마지막 채팅 메세지", example = "안녕하세요")
    String lastChatMessage
) {

}
