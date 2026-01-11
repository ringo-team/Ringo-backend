package com.lingo.lingoproject.chat;

import com.lingo.lingoproject.chat.dto.GetChatMessageResponseDto;
import com.lingo.lingoproject.chat.dto.CreateChatroomRequestDto;
import com.lingo.lingoproject.chat.dto.CreateChatroomResponseDto;
import com.lingo.lingoproject.chat.dto.GetChatResponseDto;
import com.lingo.lingoproject.chat.dto.GetChatroomResponseDto;
import com.lingo.lingoproject.domain.Chatroom;
import com.lingo.lingoproject.domain.Message;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.exception.ErrorCode;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.fcm.FcmService;
import com.lingo.lingoproject.user.UserService;
import com.lingo.lingoproject.utils.JsonListWrapper;
import com.lingo.lingoproject.utils.ResultMessageResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name= "chat-controller", description = "채팅 관련 컨트롤러")
public class ChatController {

  private final ChatService chatService;
  private final SimpMessagingTemplate simpMessagingTemplate;
  private final FcmService fcmService;

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
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "0000",
              description = "조회 성공",
              content = @Content(schema = @Schema(implementation =  GetChatResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0003",
              description = "조회 권한이 없습니다.",
              content = @Content(schema = @Schema(implementation =  ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0004",
              description = "해당 id로 채팅방을 조회할 수 없습니다.",
              content = @Content(schema = @Schema(implementation =  ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E1000",
              description = "내부 오류, 기타 문의",
              content = @Content(schema = @Schema(implementation =  ResultMessageResponseDto.class))
          )
      }
  )
  @GetMapping("/chatrooms/{roomId}/messages")
  public ResponseEntity<GetChatResponseDto> getChattingMessages(
      @Parameter(description = "채팅방 id", example = "4")
      @PathVariable(value = "roomId") Long roomId,

      @Parameter(description = "페이지 수", example = "2")
      @RequestParam(value = "page", defaultValue = "0") int page,

      @Parameter(description = "페이지 크기", example = "50")
      @RequestParam(value = "size", defaultValue = "100") int size,

      @AuthenticationPrincipal User user){

    try {

      // 조회 권한 체크
      if(!chatService.isMemberInChatroom(roomId, user.getId())){
        log.error("authUserId={}, step=잘못된_유저_요청, status=FAILED", user.getId());
        throw new RingoException("채팅방 메세지를 조회할 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
      }

      // 불러온 메세지들 중에 안 읽은 메세지를 읽음 처리한다.
      log.info("userId={}, chatroomId={}, step=메세지_읽음_처리_시작, status=SUCCESS", user.getId(), roomId);
      chatService.changeNotReadToReadMessages(roomId, user.getId());
      log.info("userId={}, chatroomId={}, step=메세지_읽음_처리_완료, status=SUCCESS", user.getId(), roomId);

      // 채팅방 매세지 조회
      log.info("userId={}, chatroomId={}, step=메세지_조회_시작, status=SUCCESS", user.getId(), roomId);
      GetChatResponseDto responses = chatService.getChatMessages(user, roomId, page, size);
      log.info("userId={}, chatroomId={}, step=메세지_조회_완료, status=SUCCESS", user.getId(), roomId);

      return ResponseEntity.status(HttpStatus.OK).body(responses);

    } catch (Exception e){
      log.error("userId={}, chatroomId={}, step=채팅방_입장_실패, status=FAILED", user.getId(), roomId, e);
      if (e instanceof RingoException re) {
        throw re;
      }
      throw new RingoException("매세지 조회에 실패하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
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

      return ResponseEntity.status(HttpStatus.CREATED).body(
          new CreateChatroomResponseDto(ErrorCode.SUCCESS.getCode(), chatroom.getId()));
    } catch (Exception e){
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("채팅방 생성에 실패하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(
      summary = "채팅방 불러오기",
      description = "채팅방 목록 불러오기"
  )
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "0000",
              description = "조회 성공"
          ),
          @ApiResponse(
              responseCode = "E0003",
              description = "채팅방 조회 권한이 없습니다.",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E1000",
              description = "내부 오류, 기타 문의",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          )
      }
  )
  @GetMapping("/users/{userId}/chatrooms")
  public ResponseEntity<JsonListWrapper<GetChatroomResponseDto>> getChatrooms(
      @Parameter(description = "유저id", example = "5")
      @PathVariable(value = "userId") Long userId,

      @AuthenticationPrincipal User user){

    try{
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

    } catch (Exception e){
      log.error("userId={}, step=채팅방_조회_실패, status=FAILED", user.getId(), e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("채팅방 조회에 실패하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * 채팅방 삭제
   */
  @Operation(
      summary = "채팅방 삭제",
      description = "채팅방, 채팅방 참여자 정보, 메세지 등을 삭제하는 api"
  )
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "0000",
              description = "삭제 성공",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0004",
              description = "해당 id로 채팅방을 찾을 수 없습니다.",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0003",
              description = "채팅방을 삭제할 권한이 없습니다.",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E1000",
              description = "내부 오류, 기타 문의",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          )
      }
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

      return ResponseEntity.status(HttpStatus.OK).body(
          new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "채팅방을 성공적으로 삭제했습니다.")
      );

    }catch (Exception e){
      log.error("userId={}, chatroomId={}, step=채팅방_삭제_실패, status=FAILED", user.getId(), roomId, e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("채팅방 삭제에 실패하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * 클라이언트가 메세지를 보낼 경로(/app/{roomId})를 적는다.
   * prefix인 app이 빠져있음
   */
  @MessageMapping("/{roomId}")
  public void sendMessage(@DestinationVariable Long roomId, GetChatMessageResponseDto chatMessageDto) {
    List<User> roomMembers = chatService.findUserInChatroom(roomId);

    if (roomMembers.size() == 1) return;

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
    fcmService.sendFcmNotification(receiver, "메세지가 도착했어요", savedMessage.getContent());
  }

}
