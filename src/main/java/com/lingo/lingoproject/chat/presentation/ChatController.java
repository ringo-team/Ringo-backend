package com.lingo.lingoproject.chat.presentation;
import com.lingo.lingoproject.chat.application.ChatNotificationEvent;
import com.lingo.lingoproject.chat.application.ChatService;
import com.lingo.lingoproject.chat.presentation.dto.GetChatMessageResponseDto;
import com.lingo.lingoproject.chat.presentation.dto.CreateChatroomRequestDto;
import com.lingo.lingoproject.chat.presentation.dto.CreateChatroomResponseDto;
import com.lingo.lingoproject.chat.presentation.dto.GetChatResponseDto;
import com.lingo.lingoproject.chat.presentation.dto.GetChatroomResponseDto;
import com.lingo.lingoproject.chat.presentation.dto.SaveAppointmentRequestDto;
import com.lingo.lingoproject.shared.domain.event.DomainEventPublisher;
import com.lingo.lingoproject.shared.domain.model.Appointment;
import com.lingo.lingoproject.shared.domain.model.Chatroom;
import com.lingo.lingoproject.shared.domain.model.Message;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import com.lingo.lingoproject.shared.utils.ApiListResponseDto;
import com.lingo.lingoproject.shared.utils.ResultMessageResponseDto;
import com.lingo.lingoproject.user.application.UserQueryUseCase;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ChatController implements ChatApi {

  private final ChatService chatService;
  private final SimpMessagingTemplate simpMessagingTemplate;
  private final DomainEventPublisher domainEventPublisher;
  private final UserQueryUseCase userQueryUseCase;
  private final RedisTemplate<String, Object> redisTemplate;


  public ResponseEntity<GetChatResponseDto> getChattingMessages(Long roomId, int page, int size, User user) {

    log.info("step=메세지_조회_시작, userId={}, chatroomId={}", user.getId(), roomId);
    GetChatResponseDto responses = chatService.getChatMessages(user, roomId, page, size);
    log.info("step=메세지_조회_완료, userId={}, chatroomId={}", user.getId(), roomId);

    return ResponseEntity.status(HttpStatus.OK).body(responses);
  }

  public ResponseEntity<CreateChatroomResponseDto> createChatRoom(@Valid @RequestBody CreateChatroomRequestDto dto) {
    Chatroom chatroom = chatService.채팅방_생성(dto);
    return ResponseEntity.status(HttpStatus.CREATED).body(new CreateChatroomResponseDto(ErrorCode.SUCCESS.getCode(), chatroom.getId()));
  }


  public ResponseEntity<ApiListResponseDto<GetChatroomResponseDto>> getChatrooms(Long userId, User user) {
    if (!userId.equals(user.getId())) {
      log.error("step=잘못된_유저_요청, authUserId={}, userId={}, status=FAILED", user.getId(), userId);
      throw new RingoException("채팅방 조회를 할 권한이 없습니다.", ErrorCode.NO_AUTH);
    }

    log.info("step=채팅방_조회_시작, userId={}", user.getId());
    List<GetChatroomResponseDto> dtos = chatService.getChatroomsByUser(user);
    log.info("step=채팅방_조회_완료, userId={}", user.getId());

    return ResponseEntity.status(HttpStatus.OK).body(new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), dtos));
  }


  public ResponseEntity<ResultMessageResponseDto> deleteChatroom(Long roomId, User user) {

    log.info("step=채팅방_삭제_시작, userId={}, chatroomId={}", user.getId(), roomId);
    chatService.채팅방_삭제(roomId, user);
    log.info("step=채팅방_삭제_완료, userId={}, chatroomId={}", user.getId(), roomId);

    return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "채팅방을 성공적으로 삭제했습니다."));
  }


  @Transactional
  public void sendMessage(Long 채팅방_id, GetChatMessageResponseDto 채팅_메세지_dto)
  {

    Long 발신자_id = 채팅_메세지_dto.getSenderId();
    User 발신자    = userQueryUseCase.유저_찾기_혹은_오류(발신자_id);
    chatService.채팅방에_속한_유저인지_검증_혹은_오류(채팅방_id, 발신자_id);

    List<User> 채팅방_접속_유저_리스트 = chatService.채팅_접속_유저_리스트_가져오기(채팅방_id);
    List<Long> 채팅방_접속_유저_ids  = 채팅방_접속_유저_리스트.stream().map(User::getId).toList();

    String 채팅_타입     = 채팅_메세지_dto.getType();
    Message 저장된_메세지 = null;

    switch (채팅_타입.toUpperCase()){
      case "DISCONNECT" -> chatService.유저_접속_세션_삭제(발신자_id);
      case "CONNECT" -> {}
      default -> 저장된_메세지 = chatService.saveMessage(채팅_메세지_dto, 채팅방_접속_유저_ids, 채팅방_id);
    }

    if (채팅_타입.equalsIgnoreCase("DISCONNECT") || 저장된_메세지 == null) return;

    Chatroom 채팅방 = chatService.채팅방_찾기_혹은_에러(채팅방_id);
    for (User 유저 : 채팅방.회원탈퇴하지_않고_채팅방_참여자인_유저_조회()) {
      try {
        log.info("step=메세지_전송, senderId={}, receiverId={}", 채팅_메세지_dto.getSenderId(), 유저.getId());
        simpMessagingTemplate.convertAndSendToUser(유저.getLoginId(), "/topic/" + 채팅방_id, 채팅_메세지_dto);
      } catch (Exception e) {
        chatService.메세지_전송_오류시_기록(e, 저장된_메세지, 유저.getLoginId(), "/topic/" + 채팅방_id);
      }
      try {
        log.info("step=채팅_미리보기_전송, chatroomId={}, userLoginId={}", 채팅방_id, 유저.getLoginId());
        simpMessagingTemplate.convertAndSendToUser(유저.getLoginId(), "/room-list", 채팅_메세지_dto);
      } catch (Exception e) {
        chatService.메세지_전송_오류시_기록(e, 저장된_메세지, 유저.getLoginId(), "/room-list/" + 채팅방_id);
      }

      if (!채팅방_접속_유저_ids.contains(유저.getId())) {
        ChatNotificationEvent event = new ChatNotificationEvent(
            유저,
            발신자,
            채팅_메세지_dto.getContent(),
            발신자.getProfile() != null ? 발신자.getProfile().getImageUrl() : null,
            채팅방_id
        );
        domainEventPublisher.publish(event);
      }
    }
  }

  @Transactional
  public ResponseEntity<ResultMessageResponseDto> 약속_잡기(SaveAppointmentRequestDto dto, User user) {
    GetChatMessageResponseDto response = chatService.약속_잡기(user, dto);
    sendMessage(dto.chatroomId(), response);

    return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "약속을 성공적으로 등록하였습니다."));
  }

  @Scheduled(fixedDelay = 60000)
  @Transactional
  public void 약속_잡기_알림_스케줄링() {
    List<Appointment> 예정된_약속들 = chatService.현재_이전에_처리되지_않은_약속_알림_가져오기();
    for (Appointment 약속 : 예정된_약속들) {
      Chatroom 채팅방 = 약속.getChatroom();
      if (채팅방 == null) continue;
      GetChatMessageResponseDto dto = GetChatMessageResponseDto.약속_예정_알림_메세지_dto_생성(약속);
      sendMessage(채팅방.getId(), dto);
      약속.setAlert(false);
    }
    chatService.약속_저장(예정된_약속들);
  }

}
