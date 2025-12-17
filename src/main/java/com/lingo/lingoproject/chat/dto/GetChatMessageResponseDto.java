package com.lingo.lingoproject.chat.dto;


import com.lingo.lingoproject.domain.Message;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
public class GetChatMessageResponseDto {
  @Schema(description = "메세지를 보낸 유저 id", example = "12")
  Long senderId;
  @Schema(description = "채팅방 id", example = "5")
  Long chatroomId;
  @Schema(description = "채팅방 크기", example = "2")
  Integer chatroomSize;
  @Schema(description = "채팅 메세지", example = "안녕하세요")
  String content;
  @Schema(description = "채팅 생성일자", example = "2025-07-05 14:46:37")
  String createdAt;
  @Schema(description = "채팅 읽은 사람 리스트", example = "[\"12\", \"25\"]")
  List<Long> readerIds;

  public static GetChatMessageResponseDto from(Long chatroomId, Message m){
    return GetChatMessageResponseDto.builder()
        .chatroomId(chatroomId)
        .senderId(m.getSenderId())
        .content(m.getContent())
        .createdAt(m.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
        .readerIds(m.getReaderIds())
        .build();
  }
}
