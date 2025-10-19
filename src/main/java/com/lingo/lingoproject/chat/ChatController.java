package com.lingo.lingoproject.chat;

import com.lingo.lingoproject.chat.dto.GetChatResponseDto;
import com.lingo.lingoproject.chat.dto.CreateChatroomDto;
import com.lingo.lingoproject.chat.dto.CreateChatroomResponseDto;
import com.lingo.lingoproject.chat.dto.GetChatroomResponseDto;
import com.lingo.lingoproject.domain.Chatroom;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.repository.UserRepository;
import com.lingo.lingoproject.utils.JsonListWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

  private final ChatService chatService;
  private final SimpMessagingTemplate simpMessagingTemplate;
  private final RedisTemplate<String, Object> redisTemplate;
  private final UserRepository userRepository;

  /**
   * 메세지 내용 불러오는 api
   * paging 처리 default로 50개 메세지 전성
   *
   * 채팅창에 있는 메세지를 모두 읽음 처리한다.
   * 유저가 어떤 {roomId}에 접속하면 레디스에 connect::{userId}::{roomId} / true 를 저장해놓는다.
   */
  @Operation(
      summary = "채팅방 메세지 불러오기",
      description = "채팅방 메세지들을 불러오는 api"
  )
  @GetMapping()
  public ResponseEntity<JsonListWrapper<GetChatResponseDto>> getChattingMessages(
      @Parameter(description = "채팅방 id", example = "4")
      @RequestParam("roomId")@NotNull Long roomId,

      @Parameter(description = "페이지 수", example = "2")
      @RequestParam(value = "page", defaultValue = "0") int page,

      @Parameter(description = "페이지 크기", example = "50")
      @RequestParam(value = "size", defaultValue = "50") int size,

      @AuthenticationPrincipal User user){
    if(!chatService.isMemberInChatroom(roomId, user.getId())){
      throw new RingoException("잘못된 접근입니다.", HttpStatus.FORBIDDEN);
    }

    chatService.changeNonReadTpReadMessages(roomId, user.getId());

    ValueOperations<String, Object> ops = redisTemplate.opsForValue();
    ops.set("connect::" + user.getId() + "::" + roomId, true);

    List<GetChatResponseDto> responses = chatService.getChattingMessages(roomId, page, size);
    return ResponseEntity.status(HttpStatus.OK).body(new JsonListWrapper<GetChatResponseDto>(responses));
  }
  /**
   * 채팅방 생성
   */
  @Operation(
      summary = "채팅방 생성",
      description = "채팅방과 채팅방 참여자 정보를 데이터베이스에 저장"
  )
  @PostMapping
  public ResponseEntity<CreateChatroomResponseDto> createChatRoom(@Valid @RequestBody CreateChatroomDto dto){
    Chatroom chatroom = chatService.createChatroom(dto);
    log.info("채팅방을 생성하였습니다. 채팅방명: {}", chatroom.getChatroomName());
    return ResponseEntity.status(HttpStatus.CREATED).body(new CreateChatroomResponseDto(
        chatroom.getId()));
  }

  @Operation(
      summary = "채팅방 불러오기",
      description = "채팅방 목록 불러오기"
  )
  @GetMapping("/{id}")
  public ResponseEntity<List<GetChatroomResponseDto>> getChatrooms(
      @Parameter(description = "유저id", example = "5")
      @PathVariable(value = "id") Long id,

      @AuthenticationPrincipal User user){
    if (!id.equals(user.getId())){
      throw new RingoException("잘못된 경로입니다.", HttpStatus.FORBIDDEN);
    }
    List<GetChatroomResponseDto> dtos = chatService.getAllChatroomByUserId(user.getId());
    return ResponseEntity.status(HttpStatus.OK).body(dtos);
  }

  /**
   * 채팅방 삭제
   */
  @Operation(
      summary = "채팅방 삭제",
      description = "채팅방, 채팅방 참여자 정보, 메세지 등을 삭제하는 api"
  )
  @DeleteMapping()
  public ResponseEntity<String> deleteChatroom(
      @Parameter(description = "채팅방 id", example = "3")
      @RequestParam("roomId")@NotNull Long roomId
  ) {
    chatService.deleteChatroom(roomId);
    log.info("채팅방을 삭제했습니다. 삭제한 채팅방 id:  {}", roomId);
    return ResponseEntity.status(HttpStatus.OK).body("채팅방을 성공적으로 삭제했습니다.");
  }
  /**
   * 클라이언트가 메세지를 보낼 경로(/app/{roomId})를 적는다.
   * prefix인 app이 빠져있음
   * 상대방이 접속해있으면 readCount 값을 1에서 0으로 바꾸어 전달한다.
   */
  @MessageMapping("/{roomId}")
  public void sendMessage(@DestinationVariable Long roomId, GetChatResponseDto message){
    List<String> usernames = chatService.getUsernamesInChatroom(roomId);
    // 메세지 저장

    List<User> roomMember = userRepository.findAllByEmailIn(usernames);
    ValueOperations<String, Object> ops = redisTemplate.opsForValue();

    // 상대방이 채팅방에 존재하는지 확인
    for (User user : roomMember) {
      if(!user.getId().equals(message.getUserId())) continue;

      if (ops.get("connect::" + user.getId() + "::" + roomId) != null) {
        message.setIsRead(true);
      } else {
        message.setIsRead(false);
      }
    }
    // 메세지 저장
    chatService.saveMessage(message, roomId);

    // 메세지 전송
    for (User user : roomMember){
      simpMessagingTemplate.convertAndSendToUser(user.getEmail(), "/topic/" + roomId, message);
    }
    // 채팅 미리보기 기능
    for (String user : usernames){
      simpMessagingTemplate.convertAndSendToUser(user, "/room-list", message);
    }

  }

  /**
   *  레디스의 접속 정보를 삭제한다.
   * @param roomId
   * @param userId
   */
  @Operation(
      summary = "채팅방 나가기",
      description = "유저가 채팅방을 나갈 때 호출하는 api"
  )
  @PatchMapping()
  public void disconnect(
      @Parameter(description = "채팅방 id", example = "5")
      @RequestParam(value = "roomId")@NotNull Long roomId,

      @Parameter(description = "유저 id", example = "5")
      @RequestParam(value = "userId")@NotNull Long userId,

      @AuthenticationPrincipal User user
  ){
    if(!userId.equals(user.getId())){
      throw new RingoException("잘못된 경로 입니다.", HttpStatus.FORBIDDEN);
    }
    ValueOperations<String, Object> ops = redisTemplate.opsForValue();
    ops.getAndDelete("connect::" + userId + "::" + roomId);
  }

}
