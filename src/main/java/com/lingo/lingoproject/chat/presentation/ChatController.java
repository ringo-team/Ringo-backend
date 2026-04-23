package com.lingo.lingoproject.chat.presentation;
import com.lingo.lingoproject.chat.application.ChatService;
import com.lingo.lingoproject.chat.presentation.dto.GetChatMessageResponseDto;
import com.lingo.lingoproject.chat.presentation.dto.CreateChatroomRequestDto;
import com.lingo.lingoproject.chat.presentation.dto.CreateChatroomResponseDto;
import com.lingo.lingoproject.chat.presentation.dto.GetChatResponseDto;
import com.lingo.lingoproject.chat.presentation.dto.GetChatroomResponseDto;
import com.lingo.lingoproject.chat.presentation.dto.SaveAppointmentRequestDto;
import com.lingo.lingoproject.shared.domain.model.Appointment;
import com.lingo.lingoproject.shared.domain.model.Chatroom;
import com.lingo.lingoproject.shared.domain.model.Message;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import com.lingo.lingoproject.notification.application.FcmNotificationUseCase;
import com.lingo.lingoproject.shared.utils.ApiListResponseDto;
import com.lingo.lingoproject.shared.utils.ResultMessageResponseDto;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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


  public ResponseEntity<GetChatResponseDto> getChattingMessages(Long roomId, int page, int size, User user) {
    chatService.validateParticipant(roomId, user.getId());

    log.info("step=메세지_조회_시작, userId={}, chatroomId={}", user.getId(), roomId);
    GetChatResponseDto responses = chatService.fetchChatMessages(user, roomId, page, size);
    log.info("step=메세지_조회_완료, userId={}, chatroomId={}", user.getId(), roomId);

    return ResponseEntity.status(HttpStatus.OK).body(responses);
  }

  public ResponseEntity<CreateChatroomResponseDto> createChatRoom(@Valid @RequestBody CreateChatroomRequestDto dto) {
    Chatroom chatroom = chatService.createChatroom(dto);
    return ResponseEntity.status(HttpStatus.CREATED).body(new CreateChatroomResponseDto(ErrorCode.SUCCESS.getCode(), chatroom.getId()));
  }


  public ResponseEntity<ApiListResponseDto<GetChatroomResponseDto>> getChatrooms(Long userId, User user) {
    if (!userId.equals(user.getId())) {
      log.error("step=잘못된_유저_요청, authUserId={}, userId={}, status=FAILED", user.getId(), userId);
      throw new RingoException("채팅방 조회를 할 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
    }

    log.info("step=채팅방_조회_시작, userId={}", user.getId());
    List<GetChatroomResponseDto> dtos = chatService.findChatroomsByUser(user);
    log.info("step=채팅방_조회_완료, userId={}", user.getId());

    return ResponseEntity.status(HttpStatus.OK).body(new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), dtos));
  }


  public ResponseEntity<ResultMessageResponseDto> deleteChatroom(Long roomId, User user) {

    chatService.validateParticipant(roomId, user.getId());

    log.info("step=채팅방_삭제_시작, userId={}, chatroomId={}", user.getId(), roomId);
    chatService.deleteChatroom(roomId);
    log.info("step=채팅방_삭제_완료, userId={}, chatroomId={}", user.getId(), roomId);

    return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "채팅방을 성공적으로 삭제했습니다."));
  }


  public void sendMessage(Long roomId, GetChatMessageResponseDto chatMessageDto) {

    chatService.validateParticipant(roomId, chatMessageDto.getSenderId());

    List<User> roomMembers = chatService.findParticipants(roomId);
    List<Long> connectedUserIdList = chatService.findConnectedUserIds(roomMembers, roomId);
    if (!connectedUserIdList.contains(chatMessageDto.getSenderId())) {
      throw new RingoException("레디스에 채팅방에 존재하는 유저 id가 없습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    String createdAt = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss").format(LocalDateTime.now());
    chatMessageDto.setReaderIds(connectedUserIdList);
    chatMessageDto.setCreatedAt(createdAt);

    Message savedMessage = null;
    if (!chatMessageDto.getType().equalsIgnoreCase("CONNECT")) {
      savedMessage = chatService.persistChatMessage(chatMessageDto, roomId);
      if (savedMessage == null) return;
    }

    for (User member : roomMembers) {
      try {
        log.info("step=메세지_전송, senderId={}, receiverId={}", chatMessageDto.getSenderId(), member.getId());
        simpMessagingTemplate.convertAndSendToUser(member.getLoginId(), "/topic/" + roomId, chatMessageDto);
      } catch (Exception e) {
        chatService.recordMessageDeliveryFailure(e, savedMessage, member.getLoginId(), "/topic/" + roomId);
      }
      try {
        simpMessagingTemplate.convertAndSendToUser(member.getLoginId(), "/room-list", chatMessageDto);
      } catch (Exception e) {
        log.error("step=채팅_미리보기_전송_실패, chatroomId={}, userLoginId={}, status=FAILED", roomId, member.getLoginId(), e);
        chatService.recordMessageDeliveryFailure(e, savedMessage, member.getLoginId(), "/room-list/" + roomId);
      }

      if (!connectedUserIdList.contains(member.getId())) {
        //fcmService.sendFcmNotification(member, "메세지가 도착했어요", savedMessage.getContent(), NotificationType.MESSAGE);
      }
    }
  }

  public ResponseEntity<ResultMessageResponseDto> saveAppointment(SaveAppointmentRequestDto dto, User user) {
    if (!dto.registerId().equals(user.getId())) {
      throw new RingoException("등록할 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
    }
    GetChatMessageResponseDto response = chatService.createAppointment(dto);
    sendMessage(response.getChatroomId(), response);

    return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "약속을 성공적으로 등록하였습니다."));
  }

  @Scheduled(fixedDelay = 60000)
  public void alertScheduling() {
    List<Appointment> alertAppointments = chatService.findDueAppointments();
    for (Appointment appointment : alertAppointments) {
      Long roomId = appointment.getChatroom().getId();
      GetChatMessageResponseDto dto = GetChatMessageResponseDto.forAppointmentAlert(appointment);
      sendMessage(roomId, dto);
      appointment.setAlert(false);
    }
    chatService.persistAppointments(alertAppointments);
  }

}
