package com.lingo.lingoproject.chat.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetChatResponseDto {
  List<GetChatroomMemberInfoResponseDto> memberInfos;
  List<GetChatMessageResponseDto> messages;
}
