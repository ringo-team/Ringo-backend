package com.lingo.lingoproject.chat;

import com.lingo.lingoproject.chat.dto.GetChatMessageResponseDto;
import com.lingo.lingoproject.chat.dto.CreateChatroomRequestDto;
import com.lingo.lingoproject.chat.dto.CreateChatroomResponseDto;
import com.lingo.lingoproject.chat.dto.GetChatResponseDto;
import com.lingo.lingoproject.chat.dto.GetChatroomResponseDto;
import com.lingo.lingoproject.domain.Chatroom;
import com.lingo.lingoproject.domain.Message;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.domain.enums.NotificationType;
import com.lingo.lingoproject.exception.ErrorCode;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.notification.FcmService;
import com.lingo.lingoproject.utils.JsonListWrapper;
import com.lingo.lingoproject.utils.ResultMessageResponseDto;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ChatController implements ChatApi{

  private final ChatService chatService;
  private final SimpMessagingTemplate simpMessagingTemplate;
  private final FcmService fcmService;


  public ResponseEntity<GetChatResponseDto> getChattingMessages(Long roomId, int page, int size, User user){

    // 조회 권한 체크
    if(!chatService.isMemberInChatroom(roomId, user.getId())){
      log.error("authUserId={}, step=잘못된_유저_요청, status=FAILED", user.getId());
      throw new RingoException("채팅방 메세지를 조회할 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
    }

    log.info("userId={}, chatroomId={}, step=메세지_조회_시작, status=SUCCESS", user.getId(), roomId);

    chatService.changeNotReadToReadMessages(roomId, user.getId());
    GetChatResponseDto responses = chatService.getChatMessages(user, roomId, page, size);

    log.info("userId={}, chatroomId={}, step=메세지_조회_완료, status=SUCCESS", user.getId(), roomId);

    return ResponseEntity.status(HttpStatus.OK).body(responses);
  }

  public ResponseEntity<CreateChatroomResponseDto> createChatRoom(
      @Valid @RequestBody CreateChatroomRequestDto dto){

    Chatroom chatroom = chatService.createChatroom(dto);

    return ResponseEntity.status(HttpStatus.CREATED).body(new CreateChatroomResponseDto(ErrorCode.SUCCESS.getCode(), chatroom.getId()));

  }


  public ResponseEntity<JsonListWrapper<GetChatroomResponseDto>> getChatrooms(Long userId, User user){

    // 조회 권한 체크
    if (!userId.equals(user.getId())){
      log.error("authUserId={}, userId={}, step=잘못된_유저_요청, status=FAILED", user.getId(), userId);
      throw new RingoException("채팅방 조회를 할 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
    }

    // 채팅방 조회
    log.info("userId={}, step=채팅방_조회_시작, status=SUCCESS", user.getId());
    List<GetChatroomResponseDto> dtos = chatService.getAllChatroomByUserId(user);
    log.info("userId={}, step=채팅방_조회_완료, status=SUCCESS", user.getId());

    return ResponseEntity.status(HttpStatus.OK).body(new JsonListWrapper<>(ErrorCode.SUCCESS.getCode(), dtos));

  }


  public ResponseEntity<ResultMessageResponseDto> deleteChatroom(Long roomId, User user) {

    log.info("userId={}, chatroomId={}, step=채팅방_삭제_시작, status=SUCCESS", user.getId(), roomId);
    chatService.deleteChatroom(roomId, user);
    log.info("userId={}, chatroomId={}, step=채팅방_삭제_완료, status=SUCCESS", user.getId(), roomId);

    return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "채팅방을 성공적으로 삭제했습니다."));

  }


  public void sendMessage(Long roomId, GetChatMessageResponseDto chatMessageDto) {
    List<User> roomMembers = chatService.findUserInChatroom(roomId);

    if (roomMembers.size() < 2) return;

    List<String> roomMemberLoginId = List.of(
        roomMembers.get(0).getLoginId(),
        roomMembers.get(1).getLoginId()
    );

    // 1 대 1 채팅이라고 가정
    // 상대방 유저 확인
    User first = roomMembers.get(0);
    User second = roomMembers.get(1);
    Long senderId = chatMessageDto.getSenderId();
    User receiver = first.getId().equals(senderId) ? second : first;

    // 상대방 유저가 채팅방에 접속 중인지 확인
    Boolean existReceiverInChatroom = chatService.existReceiverInChatroom(senderId, receiver.getId(), roomId);
    if (existReceiverInChatroom){
      chatMessageDto.setReaderIds(List.of(senderId, receiver.getId()));
    }else{
      chatMessageDto.setReaderIds(List.of(senderId));
    }

    // 메세지 저장
    Message savedMessage = chatService.saveMessage(chatMessageDto, roomId);

    if (savedMessage == null) return;

    for (String userLoginId : roomMemberLoginId) {
      try {
        // 해당 방에 메세지 전송
        simpMessagingTemplate.convertAndSendToUser(userLoginId, "/topic/" + roomId, chatMessageDto);
      } catch (Exception e) {
        // 에러, 메세지, 방id, 전송자, 목적지
        chatService.savedSimpMessagingError(e, savedMessage, roomId, userLoginId, "/topic/");
      }
      try {
        // 채팅 미리보기 기능
        simpMessagingTemplate.convertAndSendToUser(userLoginId, "/room-list", chatMessageDto);
      } catch (Exception e) {
        log.error("chatroomId={}, userLoginId={}, step=메세지_전송_실패, status=FAILED", roomId, userLoginId,
            e);
        chatService.savedSimpMessagingError(e, savedMessage, roomId, userLoginId, "/room-list/");
      }
    }

    // 방에 없는 유저에게 fcm 메세지 전달
    if (existReceiverInChatroom) return;
    fcmService.sendFcmNotification(receiver, "메세지가 도착했어요", savedMessage.getContent(), NotificationType.MESSAGE);
  }

}
