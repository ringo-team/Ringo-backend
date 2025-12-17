package com.lingo.lingoproject.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetChatResponseDto {
  @Schema(description = "응답 결과", example = "0000")
  String result;
  GetChatroomMemberInfoResponseDto memberInfo;
  List<GetChatMessageResponseDto> messages;
}
