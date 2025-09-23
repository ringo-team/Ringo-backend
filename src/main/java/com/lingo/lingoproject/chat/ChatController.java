package com.lingo.lingoproject.chat;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

  private final ChatService chatService;
  /*
   * 메세지 내용 불러오는 api
   * paging 처리 default로 50개 메세지 전성
   */
  @GetMapping("/messages")
  public ResponseEntity<?> getChattingMessages( @RequestParam("roomId") Long roomId,
                                                @RequestParam(value = "page", defaultValue = "0") int page,
                                                @RequestParam(value = "size", defaultValue = "50") int size){
    List<ChatResponseDto> response = chatService.getChattingMessages(roomId, page, size);
    return ResponseEntity.status(HttpStatus.OK).body(response);
  }
  /*
   * 채팅방 생성
   */
  /*
   * 채팅방 삭제
   */
  /*
   * 채팅방 가져오기
   * userId에 해당하는 채팅방 정보를 모두 불러온다.
   */

  /*
   * messageMapping에는 메세지를 받을 경로를 적는다.
   * 메세지 받을 경로의 prefix인 app이 빠져있음
   */
  @MessageMapping("/{roomId}")
  /*
   * broker의 경로를 정하여 해당 경로로 메세지를 보냄
   * broker의 prefix는 설정한대로 send를 붙여줌
   */
  @SendTo("/broker/{roomId}")
  public ChatResponseDto sendMessage(@DestinationVariable Long roomId, ChatResponseDto message){
    // 메세지 저장
    chatService.saveMessage(message, roomId);
    return message;
  }


}
