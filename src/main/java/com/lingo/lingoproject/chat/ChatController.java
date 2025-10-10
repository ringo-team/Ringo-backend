package com.lingo.lingoproject.chat;

import com.lingo.lingoproject.chat.dto.ChatResponseDto;
import com.lingo.lingoproject.chat.dto.CreateChatroomDto;
import com.lingo.lingoproject.chat.dto.CreateChatroomResponseDto;
import com.lingo.lingoproject.domain.Chatroom;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

  private final ChatService chatService;
  /**
   * 메세지 내용 불러오는 api
   * paging 처리 default로 50개 메세지 전성
   */
  @Operation(
      summary = "채팅방 불러오기",
      description = "채팅방 메세지들을 불러오는 api"
  )
  @GetMapping("/messages")
  public ResponseEntity<?> getChattingMessages( @RequestParam("roomId") Long roomId,
                                                @RequestParam(value = "page", defaultValue = "0") int page,
                                                @RequestParam(value = "size", defaultValue = "50") int size){
    List<ChatResponseDto> response = chatService.getChattingMessages(roomId, page, size);
    return ResponseEntity.status(HttpStatus.OK).body(response);
  }
  /**
   * 채팅방 생성
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseEntity<?> createChatRoom(@RequestBody CreateChatroomDto dto){
    Chatroom chatroom = chatService.createChatroom(dto);
    log.info("채팅방을 생성하였습니다. 채팅방명: {}", chatroom.getChatroomName());
    return ResponseEntity.status(HttpStatus.CREATED).body(new CreateChatroomResponseDto(
        chatroom.getId()));
  }
  /**
   * 채팅방 삭제
   */
  @DeleteMapping()
  public ResponseEntity<?> deleteChatroom(@RequestParam("roomId") Long roomId){
    chatService.deleteChatroom(roomId);
    log.info("채팅방을 삭제했습니다. 삭제한 채팅방 id:  {}", roomId);
    return ResponseEntity.status(HttpStatus.OK).body("채팅방을 성공적으로 삭제했습니다.");
  }
  /**
   * messageMapping에는 메세지를 받을 경로를 적는다.
   * 메세지 받을 경로의 prefix인 app이 빠져있음
   */
  @MessageMapping("/{roomId}")
  /**
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
