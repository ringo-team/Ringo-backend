package com.lingo.lingoproject.chat.dto;

import com.lingo.lingoproject.domain.enums.ChatType;

public record CreateChatroomDto(Long user1Id, Long user2Id, ChatType chatType) {

}
