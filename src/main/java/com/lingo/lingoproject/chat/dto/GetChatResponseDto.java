package com.lingo.lingoproject.chat.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetChatResponseDto {
  @Schema(description = "유저 id", example = "12")
  Long userId;
  @Schema(description = "채팅 메세지", example = "안녕하세요")
  String content;
  @Schema(description = "채팅 생성일자", example = "2025-07-05 14:46:37")
  String createdAt;
  @Schema(description = "채팅 읽음 여부", example = "true")
  Boolean isRead;
}
