package com.lingo.lingoproject.api.chat;

import com.lingo.lingoproject.api.chat.dto.GetChatMessageResponseDto;
import com.lingo.lingoproject.api.chat.dto.CreateChatroomRequestDto;
import com.lingo.lingoproject.api.chat.dto.CreateChatroomResponseDto;
import com.lingo.lingoproject.api.chat.dto.GetChatResponseDto;
import com.lingo.lingoproject.api.chat.dto.GetChatroomResponseDto;
import com.lingo.lingoproject.api.chat.dto.SaveAppointmentRequestDto;
import com.lingo.lingoproject.db.domain.Appointment;
import com.lingo.lingoproject.db.domain.Chatroom;
import com.lingo.lingoproject.db.domain.Message;
import com.lingo.lingoproject.db.domain.User;
import com.lingo.lingoproject.common.exception.ErrorCode;
import com.lingo.lingoproject.common.exception.RingoException;
import com.lingo.lingoproject.common.notification.FcmService;
import com.lingo.lingoproject.common.utils.ApiListResponseDto;
import com.lingo.lingoproject.common.utils.ResultMessageResponseDto;
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
public class ChatController implements ChatApi{

  private final ChatService chatService;
  private final SimpMessagingTemplate simpMessagingTemplate;
  private final FcmService fcmService;


  public ResponseEntity<GetChatResponseDto> getChattingMessages(Long roomId, int page, int size, User user){

    // 조회 권한 체크
    if(!chatService.isMemberInChatroom(roomId, user.getId())){
      log.error("""
          
          message=채팅방 메세지를 조회할 권한이 없습니다.
          authUserId={},
          step=잘못된_유저_요청,
          status=FAILED
          
          """, user.getId());
      throw new RingoException("채팅방 메세지를 조회할 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
    }

    log.info("""

          userId={},
          chatroomId={},
          step=메세지_조회_시작,
          status=SUCCESS

          """, user.getId(), roomId);

    chatService.readAllMessages(roomId, user.getId());
    GetChatResponseDto responses = chatService.getChatMessages(user, roomId, page, size);

    log.info("""

          userId={},
          chatroomId={},
          step=메세지_조회_완료,
          status=SUCCESS

          """, user.getId(), roomId);

    return ResponseEntity.status(HttpStatus.OK).body(responses);
  }

  public ResponseEntity<CreateChatroomResponseDto> createChatRoom(
      @Valid @RequestBody CreateChatroomRequestDto dto){

    Chatroom chatroom = chatService.createChatroom(dto);

    return ResponseEntity.status(HttpStatus.CREATED).body(new CreateChatroomResponseDto(ErrorCode.SUCCESS.getCode(), chatroom.getId()));

  }


  public ResponseEntity<ApiListResponseDto<GetChatroomResponseDto>> getChatrooms(Long userId, User user){

    // 조회 권한 체크
    if (!userId.equals(user.getId())){
      log.error("""

          authUserId={},
          userId={},
          step=잘못된_유저_요청,
          status=FAILED

          """, user.getId(), userId);
      throw new RingoException("채팅방 조회를 할 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
    }

    // 채팅방 조회
    log.info("""

          userId={},
          step=채팅방_조회_시작,
          status=SUCCESS

          """, user.getId());
    List<GetChatroomResponseDto> dtos = chatService.getAllChatroomByUserId(user);
    log.info("""

          userId={},
          step=채팅방_조회_완료,
          status=SUCCESS

          """, user.getId());

    return ResponseEntity.status(HttpStatus.OK).body(new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), dtos));

  }


  public ResponseEntity<ResultMessageResponseDto> deleteChatroom(Long roomId, User user) {

    log.info("""

          userId={},
          chatroomId={},
          step=채팅방_삭제_시작,
          status=SUCCESS

          """, user.getId(), roomId);
    chatService.deleteChatroom(roomId, user);
    log.info("""

          userId={},
          chatroomId={},
          step=채팅방_삭제_완료,
          status=SUCCESS

          """, user.getId(), roomId);

    return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "채팅방을 성공적으로 삭제했습니다."));

  }


  public void sendMessage(Long roomId, GetChatMessageResponseDto chatMessageDto) {
    List<User> roomMembers = chatService.getParticipantsInChatroom(roomId);


    List<Long> connectedUserIdList = chatService.getConnectedUserIdList(roomMembers, roomId);
    if (!connectedUserIdList.contains(chatMessageDto.getSenderId()
    )){
      throw new RingoException("레디스에 채팅방에 존재하는 유저 id가 없습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    String createdAt = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss").format(LocalDateTime.now());
    chatMessageDto.setReaderIds(connectedUserIdList);
    chatMessageDto.setCreatedAt(createdAt);

    // 메세지 저장
    Message savedMessage = null;
    if(!chatMessageDto.getType().equalsIgnoreCase("CONNECT")) {
      savedMessage = chatService.saveMessage(chatMessageDto, roomId);
      if (savedMessage == null) return;
    }

    for (User member : roomMembers) {
      try {
        if (connectedUserIdList.contains(member.getId())) chatMessageDto.setIsRead(1);
        else chatMessageDto.setIsRead(0);
        // 해당 방에 메세지 전송

        log.info("senderID: " + chatMessageDto.getSenderId() + ", receiverID: " + member.getId());
        simpMessagingTemplate.convertAndSendToUser(member.getLoginId(), "/topic/" + roomId, chatMessageDto);
      } catch (Exception e) {
        // 에러, 메세지, 방id, 전송자, 목적지
        chatService.savedSimpMessagingError(e, savedMessage, roomId, member.getLoginId(), "/topic/");
      }
      try {
        // 채팅 미리보기 기능
        simpMessagingTemplate.convertAndSendToUser(member.getLoginId(), "/room-list", chatMessageDto);
      } catch (Exception e) {
        log.error("""

          chatroomId={},
          userLoginId={},
          step=메세지_전송_실패,
          status=FAILED

          """, roomId, member.getLoginId(), e);
        chatService.savedSimpMessagingError(e, savedMessage, roomId, member.getLoginId(), "/room-list/");
      }

      if (!connectedUserIdList.contains(member.getId())) {
        //fcmService.sendFcmNotification(member, "메세지가 도착했어요", savedMessage.getContent(), NotificationType.MESSAGE);
      }
    }
  }

  public ResponseEntity<ResultMessageResponseDto> saveAppointment(SaveAppointmentRequestDto dto, User user){
    if (!dto.registerId().equals(user.getId())){
      throw new RingoException("등록할 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
    }
    GetChatMessageResponseDto response = chatService.saveAppointment(dto);
    sendMessage(response.getChatroomId(), response);

    return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "약속을 성공적으로 등록하였습니다."));
  }

  @Scheduled(fixedDelay = 60000)
  public void alertScheduling(){
    List<Appointment> alertAppointments = chatService.getScheduledAppointments();
    for (Appointment appointment : alertAppointments){
      Long roomId = appointment.getChatroom().getId();
      GetChatMessageResponseDto dto = GetChatMessageResponseDto.builder()
          .content("오늘 약속이 예정되어 있어요")
          .appointmentTime(appointment.getAppointmentTime())
          .place(appointment.getPlace())
          .type("APPOINTMENT")
          .build();
      sendMessage(roomId, dto);
      appointment.setAlert(false);
    }
    chatService.saveAppointment(alertAppointments);
  }

}
