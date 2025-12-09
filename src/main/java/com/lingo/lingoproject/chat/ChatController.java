package com.lingo.lingoproject.chat;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import com.lingo.lingoproject.chat.dto.GetChatResponseDto;
import com.lingo.lingoproject.chat.dto.CreateChatroomRequestDto;
import com.lingo.lingoproject.chat.dto.CreateChatroomResponseDto;
import com.lingo.lingoproject.chat.dto.GetChatroomResponseDto;
import com.lingo.lingoproject.domain.Chatroom;
import com.lingo.lingoproject.domain.ExceptionMessage;
import com.lingo.lingoproject.domain.FailedMessageLog;
import com.lingo.lingoproject.domain.Message;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.repository.ExceptionMessageRepository;
import com.lingo.lingoproject.repository.FailedMessageLogRepository;
import com.lingo.lingoproject.repository.FcmTokenRepository;
import com.lingo.lingoproject.repository.UserRepository;
import com.lingo.lingoproject.utils.JsonListWrapper;
import com.lingo.lingoproject.utils.ResultMessageResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import java.util.ArrayList;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ChatController {

  private final ChatService chatService;
  private final SimpMessagingTemplate simpMessagingTemplate;
  private final RedisTemplate<String, Object> redisTemplate;
  private final UserRepository userRepository;
  private final FcmTokenRepository fcmTokenRepository;
  private final ExceptionMessageRepository exceptionMessageRepository;
  private final FailedMessageLogRepository failedMessageLogRepository;

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
  @GetMapping("/chatrooms/{roomId}/messages")
  public ResponseEntity<JsonListWrapper<GetChatResponseDto>> getChattingMessages(
      @Parameter(description = "채팅방 id", example = "4")
      @PathVariable(value = "roomId") Long roomId,

      @Parameter(description = "페이지 수", example = "2")
      @RequestParam(value = "page", defaultValue = "0") int page,

      @Parameter(description = "페이지 크기", example = "50")
      @RequestParam(value = "size", defaultValue = "100") int size,

      @AuthenticationPrincipal User user){
    if(!chatService.isMemberInChatroom(roomId, user.getId())){
      log.error("authUserId={}, step=잘못된_유저_요청, status=FAILED", user.getId());
      throw new RingoException("채팅방 메세지를 조회할 권한이 없습니다.", HttpStatus.FORBIDDEN);
    }

    try {
      // 유저 채팅방 레디스에 저장
      log.info("userId={}, chatroomId={}, step=채팅방_입장, status=SUCCESS", user.getId(), roomId);
      ValueOperations<String, Object> ops = redisTemplate.opsForValue();
      ops.set("connect::" + user.getId() + "::" + roomId, true);

      // 불러온 메세지들 중에 안 읽은 메세지를 읽음 처리한다.
      log.info("userId={}, chatroomId={}, step=메세지_읽음_처리_시작, status=SUCCESS", user.getId(), roomId);
      chatService.changeNotReadToReadMessages(roomId, user.getId());
      log.info("userId={}, chatroomId={}, step=메세지_읽음_처리_완료, status=SUCCESS", user.getId(), roomId);

      // 채팅방 매세지 조회
      log.info("userId={}, chatroomId={}, step=메세지_조회_시작, status=SUCCESS", user.getId(), roomId);
      List<GetChatResponseDto> responses = chatService.getChatMessages(roomId, page, size);
      log.info("userId={}, chatroomId={}, step=메세지_조회_완료, status=SUCCESS", user.getId(), roomId);

      return ResponseEntity.status(HttpStatus.OK).body(new JsonListWrapper<>(responses));

    } catch (Exception e){
      log.error("userId={}, chatroomId={}, step=채팅방_입장_실패, status=FAILED", user.getId(), roomId, e);
      if (e instanceof RingoException re) {
        throw re;
      }
      throw new RingoException("매세지 조회에 실패하였습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * 채팅방 생성
   */
  @Operation(
      summary = "채팅방 생성",
      description = "채팅방과 채팅방 참여자 정보를 데이터베이스에 저장"
  )
  @PostMapping("/chatrooms")
  public ResponseEntity<CreateChatroomResponseDto> createChatRoom(
      @Valid @RequestBody CreateChatroomRequestDto dto){
    try {
      Chatroom chatroom = chatService.createChatroom(dto);
      return ResponseEntity.status(HttpStatus.CREATED).body(new CreateChatroomResponseDto(chatroom.getId()));
    } catch (Exception e){
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("채팅방 생성에 실패하였습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(
      summary = "채팅방 불러오기",
      description = "채팅방 목록 불러오기"
  )
  @GetMapping("/users/{userId}/chatrooms")
  public ResponseEntity<List<GetChatroomResponseDto>> getChatrooms(
      @Parameter(description = "유저id", example = "5")
      @PathVariable(value = "userId") Long userId,

      @AuthenticationPrincipal User user){
    if (!userId.equals(user.getId())){
      log.error("authUserId={}, userId={}, step=잘못된_유저_요청, status=FAILED", user.getId(), userId);
      throw new RingoException("채팅방 조회를 할 권한이 없습니다.", HttpStatus.FORBIDDEN);
    }
    try{

      log.info("userId={}, step=채팅방_조회_시작, status=SUCCESS", user.getId());
      List<GetChatroomResponseDto> dtos = chatService.getAllChatroomByUserId(user);
      log.info("userId={}, step=채팅방_조회_완료, status=SUCCESS", user.getId());
      return ResponseEntity.status(HttpStatus.OK).body(dtos);

    } catch (Exception e){
      log.error("userId={}, step=채팅방_조회_실패, status=FAILED", user.getId(), e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("채팅방 조회에 실패하였습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * 채팅방 삭제
   */
  @Operation(
      summary = "채팅방 삭제",
      description = "채팅방, 채팅방 참여자 정보, 메세지 등을 삭제하는 api"
  )
  @DeleteMapping("/chatrooms/{roomId}")
  public ResponseEntity<ResultMessageResponseDto> deleteChatroom(
      @Parameter(description = "채팅방 id", example = "3")
      @PathVariable("roomId") Long roomId,

      @AuthenticationPrincipal User user
  ) {
    try {

      log.info("userId={}, chatroomId={}, step=채팅방_삭제_시작, status=SUCCESS", user.getId(), roomId);
      chatService.deleteChatroom(roomId, user);
      log.info("userId={}, chatroomId={}, step=채팅방_삭제_완료, status=SUCCESS", user.getId(), roomId);

      return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto("채팅방을 성공적으로 삭제했습니다."));

    }catch (Exception e){
      log.error("userId={}, chatroomId={}, step=채팅방_삭제_실패, status=FAILED", user.getId(), roomId, e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("채팅방 삭제에 실패하였습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * 클라이언트가 메세지를 보낼 경로(/app/{roomId})를 적는다.
   * prefix인 app이 빠져있음
   */
  @MessageMapping("/{roomId}")
  public void sendMessage(@DestinationVariable Long roomId, GetChatResponseDto message)
      throws FirebaseMessagingException {

    List<String> userEmails = null;
    List<User> roomMembers = null;
    List<Long> existMemberIdList = new ArrayList<>();
    Message savedMessage = null;

    try {

      ValueOperations<String, Object> ops = redisTemplate.opsForValue();

      userEmails = chatService.getUserEmailsInChatroom(roomId);
      roomMembers = userRepository.findAllByEmailIn(userEmails);

      // 현재 접속해있는 채팅방 멤버의 유저 id를 구한다.
      for (User user : roomMembers) {

        if (ops.get("connect::" + user.getId() + "::" + roomId) != null) {

          existMemberIdList.add(user.getId());
        }
      }
      message.setReaderIds(existMemberIdList);
      // 메세지 저장
      savedMessage = chatService.saveMessage(message, roomId);

    }catch (Exception e){
      log.error("senderId={}, chatroomId={}, step=메세제_저장, status=FAILED",  message.getSenderId(), roomId, e);
      return;
    }


    for (String userEmail : userEmails){
      try {
        // 해당 방에 메세지 전송
        simpMessagingTemplate.convertAndSendToUser(userEmail, "/topic/" + roomId, message);
      } catch (Exception e) {
        log.error("chatroomId={}, userEmail={}, step=메세지_전송_실패, status=FAILED", roomId, userEmail, e);
        FailedMessageLog failedMessageLog = FailedMessageLog.builder()
            .roomId(roomId)
            .errorMessage(e.getMessage())
            .errorCause(e.getCause() != null ? e.getCause().getMessage() : null)
            .messageId(savedMessage.getId())
            .destination("/topic/" + roomId)
            .userEmail(userEmail)
            .build();
        failedMessageLogRepository.save(failedMessageLog);
      }
      try{
        // 채팅 미리보기 기능
        simpMessagingTemplate.convertAndSendToUser(userEmail, "/room-list", message);
      }catch(Exception e){
        log.error("chatroomId={}, userEmail={}, step=메세지_전송_실패, status=FAILED", roomId, userEmail, e);
        FailedMessageLog failedMessageLog = FailedMessageLog.builder()
            .roomId(roomId)
            .errorMessage(e.getMessage())
            .errorCause(e.getCause() != null ? e.getCause().getMessage() : null)
            .messageId(savedMessage.getId())
            .destination("/room-list")
            .userEmail(userEmail)
            .build();
        failedMessageLogRepository.save(failedMessageLog);
      }
    }

    // 채팅방에 존재하지 않는 사람들은 fcm으로 알림을 보낸다.
    List<User> notExistMember = roomMembers.stream()
        .filter(u -> !existMemberIdList.contains(u.getId()))
        .toList();
    List<String> fcmTokens = null;
    try {
      fcmTokens = fcmTokenRepository.findByUserIn(notExistMember);
    }catch (Exception e){
      log.error("chatroomId={}, step=FCM_TOKEN_조회, status=FAILED", roomId, e);
      return;
    }
    if(!fcmTokens.isEmpty()) {
      /*
       * {
       *    "message":{
       *      "notification":{
       *        "title": "title",
       *        "body": "message"
       *        "image": "imageUrl"
       *      }
       *    }
       * }
       */
      MulticastMessage multicastMessage = MulticastMessage.builder()
          .addAllTokens(fcmTokens)
          .setNotification(Notification.builder()
              .setTitle("메세지가 도착했습니다.")
              .setBody(message.getContent())
              .setImage("imageUrl")
              .build())
        .build();
      BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(multicastMessage);
      // 이미지가 전송되지 않으면 몇개의 이미지가 전송되지 않았는지 데이터베이스에 저장
      if(response.getFailureCount() > 0){
        List<SendResponse> responses = response.getResponses();
        List<FailedMessageLog> errorLogList = new ArrayList<>();
        for (SendResponse sendResponse : responses) {
          if (!sendResponse.isSuccessful()){
            // 보내지지 않은 메세지의 세부 정보를 로깅하기
            FirebaseMessagingException exception = sendResponse.getException();
            log.error("chatroomId={}, step=CHAT_FCM_MULTICAST, status=FAILED", roomId);
            FailedMessageLog log = FailedMessageLog.builder()
                .roomId(roomId)
                .errorMessage(exception.getMessage())
                .errorCause(exception.getCause() != null ? exception.getCause().getMessage() : null)
                .messageId(savedMessage.getId())
                .destination("fcm")
                .build();
            errorLogList.add(log);
          }
        }
        failedMessageLogRepository.saveAll(errorLogList);
        ExceptionMessage exceptionMessage = new ExceptionMessage(roomId + "에 " + errorLogList.size() + "개의 메세지가 보내지지 않았습니다.");
        exceptionMessageRepository.save(exceptionMessage);
      }
    }

  }

  /**
   *  레디스의 접속 정보를 삭제한다.
   * @param roomId
   */
  @Operation(
      summary = "채팅방 나가기",
      description = "유저가 채팅방을 나갈 때 호출하는 api"
  )
  @PatchMapping("/chatrooms/{roomId}/leave")
  public ResponseEntity<ResultMessageResponseDto> disconnect(
      @Parameter(description = "채팅방 id", example = "5")
      @PathVariable(value = "roomId") Long roomId,

      @AuthenticationPrincipal User user
  ){
    try {

      // 유저가 채팅방을 나가면 레디스에 저장했던 접속 정보를 삭제한다.
      // Delete connect::userId::roomId
      ValueOperations<String, Object> ops = redisTemplate.opsForValue();
      ops.getAndDelete("connect::" + user.getId() + "::" + roomId);
      log.info("userId={}, chatroomId={}, step=채팅방_나가기 ,status=SUCCESS", user.getId(), roomId);

      return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto("유저가 채팅방을 나갔습니다."));

    }catch (Exception e){
      log.error("userId={}, chatroomId={}, step=채팅방_나가기_실패 ,status=FAILED", user.getId(), roomId);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("채팅방을 나가기 실패하였습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
    }

  }

}
