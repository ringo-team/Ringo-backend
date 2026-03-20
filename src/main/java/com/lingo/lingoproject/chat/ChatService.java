package com.lingo.lingoproject.chat;


import com.lingo.lingoproject.chat.dto.GetChatMessageResponseDto;
import com.lingo.lingoproject.chat.dto.CreateChatroomRequestDto;
import com.lingo.lingoproject.chat.dto.GetChatResponseDto;
import com.lingo.lingoproject.chat.dto.GetChatroomMemberInfoResponseDto;
import com.lingo.lingoproject.chat.dto.GetChatroomResponseDto;
import com.lingo.lingoproject.chat.dto.SaveAppointmentRequestDto;
import com.lingo.lingoproject.domain.Appointment;
import com.lingo.lingoproject.domain.Chatroom;
import com.lingo.lingoproject.domain.ChatroomParticipant;
import com.lingo.lingoproject.domain.FailedChatMessageLog;
import com.lingo.lingoproject.domain.Hashtag;
import com.lingo.lingoproject.domain.Message;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.domain.enums.ChatType;
import com.lingo.lingoproject.domain.enums.MatchingStatus;
import com.lingo.lingoproject.exception.ErrorCode;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.repository.AppointmentRepository;
import com.lingo.lingoproject.repository.ChatroomParticipantRepository;
import com.lingo.lingoproject.repository.ChatroomRepository;
import com.lingo.lingoproject.mongo_repository.MessageRepository;
import com.lingo.lingoproject.repository.FailedChatMessageLogRepository;
import com.lingo.lingoproject.repository.HashtagRepository;
import com.lingo.lingoproject.repository.MatchingRepository;
import com.lingo.lingoproject.repository.UserRepository;
import com.lingo.lingoproject.utils.GenericUtils;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.swing.text.html.Option;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

  private final ChatroomRepository chatroomRepository;
  private final MessageRepository messageRepository;
  private final UserRepository userRepository;
  private final ChatroomParticipantRepository chatroomParticipantRepository;
  private final MatchingRepository matchingRepository;
  private final RedisTemplate<String, Object> redisTemplate;
  private final HashtagRepository hashtagRepository;
  private final FailedChatMessageLogRepository failedChatMessageLogRepository;
  private final AppointmentRepository appointmentRepository;

  
  public GetChatResponseDto getChatMessages(User user, Long chatroomId, int page, int size){
    // 페이지네이션
    Pageable pageable = PageRequest.of(page, size);

    // 메세지 조회
    Page<Message> messages = messageRepository.findAllByChatroomIdOrderByCreatedAtDesc(chatroomId, pageable);
    List<GetChatMessageResponseDto> messagesDto = messages.stream()
        .map(m -> GetChatMessageResponseDto.from(chatroomId, m))
        .toList();

    // 채팅방 조회
    Chatroom chatroom = chatroomRepository.findById(chatroomId)
        .orElseThrow(() -> new RingoException("채팅방을 찾을 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));

    // 채팅방 멤버 조회
    List<ChatroomParticipant> participants = chatroomParticipantRepository.findAllByChatroom(chatroom);
    if (participants.size() == 1){
      return GetChatResponseDto.builder()
          .result(ErrorCode.SUCCESS.getCode())
          .memberInfo(null)
          .messages(messagesDto)
          .build();
    }

    // 상대방 유저 확인
    User first = participants.get(0).getParticipant();
    User second = participants.get(1).getParticipant();
    User otherChatroomMember = first.getId().equals(user.getId()) ? second : first;

    List<String> hashtags = hashtagRepository.findAllByUser(otherChatroomMember).stream()
        .map(Hashtag::getHashtag).toList();
    GetChatroomMemberInfoResponseDto memberInfo = GetChatroomMemberInfoResponseDto.builder()
        .profileUrl(otherChatroomMember.getProfile().getImageUrl())
        .userId(otherChatroomMember.getId())
        .nickname(otherChatroomMember.getNickname())
        .hashtag(hashtags)
        .build();

    return GetChatResponseDto.builder()
        .result(ErrorCode.SUCCESS.getCode())
        .memberInfo(memberInfo)
        .messages(messagesDto)
        .build();
  }

  public List<Long> getExistChatroomMemberIdList(List<User> roomMembers, Long roomId) {

    try {

      List<Long> result = new ArrayList<>();

      for (User member : roomMembers){
        String key = "connect::" + member.getId() + "::" + roomId;

        if (redisTemplate.hasKey(key)) result.add(member.getId());
      }

      return result;

    }catch (Exception e){
      log.error("chatroomId={}, step=메세제_저장, status=FAILED", roomId, e);
      throw new RingoException("채팅방에 접속해있는 멤버 확인 중 오류가 발생했습니다.", ErrorCode.INTERNAL_SERVER_ERROR,  HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  public void savedSimpMessagingError(
      Exception e,
      Message savedMessage,
      Long roomId,
      String userLoginId,
      String destination
  ) {
    log.error("chatroomId={}, userLoginId={}, step=메세지_전송_실패, status=FAILED", roomId, userLoginId,
        e);
    FailedChatMessageLog failedFcmMessageLog = FailedChatMessageLog.builder()
        .roomId(roomId)
        .errorMessage(e.getMessage())
        .errorCause(e.getCause() != null ? e.getCause().getMessage() : null)
        .messageId(savedMessage.getId())
        .destination(destination + roomId)
        .userLoginId(userLoginId)
        .build();
    failedChatMessageLogRepository.save(failedFcmMessageLog);
  }

  public Message saveMessage(GetChatMessageResponseDto message, Long chatroomId){
    Message messageEntity = Message.builder()
        .id(UUID.randomUUID().toString())
        .chatroomId(chatroomId)
        .senderId(message.getSenderId())
        .content(message.getContent())
        .readerIds(message.getReaderIds())
        .createdAt(LocalDateTime.now())
        .build();
    return messageRepository.save(messageEntity);
  }

  public Chatroom createChatroom(CreateChatroomRequestDto dto){

    // 채팅 타입 검사
    ChatType chatType = GenericUtils.validateAndReturnEnumValue(ChatType.values(), dto.chatType());

    // 유저 조회
    User user1 = userRepository.findById(dto.user1Id())
        .orElseThrow(() -> new RingoException("id에 해당하는 유저를 찾을 수 없습니다.", ErrorCode.NOT_FOUND_USER, HttpStatus.BAD_REQUEST));
    User user2 = userRepository.findById(dto.user2Id())
        .orElseThrow(() -> new RingoException("id에 해당하는 유저를 찾을 수 없습니다.", ErrorCode.NOT_FOUND_USER, HttpStatus.BAD_REQUEST));

    // 매칭된 유저가 아니면 채팅방을 생성할 수 없다.
    if (!(matchingRepository.existsByRequestUserAndRequestedUserAndMatchingStatus(user1, user2, MatchingStatus.ACCEPTED)
        || matchingRepository.existsByRequestUserAndRequestedUserAndMatchingStatus(user2, user1, MatchingStatus.ACCEPTED))
    ){
      throw new RingoException("매칭되지 않은 쌍은 채팅방을 생성할 수 없습니다.", ErrorCode.NO_AUTH, HttpStatus.BAD_REQUEST);
    }

    // 채팅방 생성 및 저장
    Chatroom chatroom = Chatroom.builder()
        .chatroomName(dto.user1Id() + "_" +dto.user2Id())
        .type(chatType)
        .build();
    Chatroom savedChatroom = chatroomRepository.save(chatroom);

    // 채팅방 참여자 생성 및 저장
    ChatroomParticipant participant1 = ChatroomParticipant.builder()
        .participant(user1)
        .chatroom(savedChatroom)
        .build();
    ChatroomParticipant participant2 = ChatroomParticipant.builder()
        .participant(user2)
        .chatroom(savedChatroom)
        .build();
    chatroomParticipantRepository.saveAll(List.of(participant1, participant2));

    return savedChatroom;
  }

  public List<GetChatroomResponseDto> getAllChatroomByUserId(User user){
    // 채팅방 조회
    List<Chatroom> chatrooms = chatroomRepository.findAllByUser(user);

    List<GetChatroomResponseDto> responseDtoList = new ArrayList<>();

    for(Chatroom chatroom : chatrooms){

      // 채팅 상대방 유저 닉네임 조회
      List<ChatroomParticipant> participants = chatroomParticipantRepository.findAllByChatroom(chatroom);

      String nickname = null;
      String imageUrl = null;

      ChatroomParticipant p1 = participants.get(0);
      ChatroomParticipant p2 = participants.get(1);

      if (!isMemberInChatroom(chatroom.getId(), user.getId())){
        throw new RingoException("채팅방에 소속되지 않은 유저이거나 탈퇴된 유저입니다.", ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST);
      }

      if (p1.isWithdrawn() || p2.isWithdrawn()){
        nickname = "알 수 없음";
      }
      else{
        User chatOpponent = p1.getParticipant().getId().equals(user.getId()) ? p2.getParticipant() : p1.getParticipant();
        nickname = chatOpponent.getNickname();
        imageUrl = chatOpponent.getProfile().getImageUrl();
      }

      // 유저가 읽지 않은 메세지의 개수를 조회한다.
      int numberOfNotReadMessages = messageRepository.findNumberOfNotReadMessages(chatroom.getId(), user.getId());

      // 채팅방 마지막 메세지를 조회한다.
      Optional<Message> lastMessage = messageRepository.findFirstByChatroomIdOrderByCreatedAtDesc(chatroom.getId());

      // 마지막 메세지 전송 시기
      String lastSendDateTime = null;
      if (lastMessage.isPresent()){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        lastSendDateTime = formatter.format(lastMessage.get().getCreatedAt());
      }

      responseDtoList.add(GetChatroomResponseDto.builder()
          .chatroomId(chatroom.getId())
          .chatOpponent(nickname)
          .chatOpponentProfileUrl(imageUrl)
          .lastChatMessage(lastMessage.map(Message::getContent).orElse(null))
          .NumberOfNotReadMessages(numberOfNotReadMessages)
          .lastSendDateTime(lastSendDateTime)
          .chatroomSize(2)
          .build());
    }
    return responseDtoList;
  }

  @Transactional
  public void deleteChatroom(Long chatroomId, User user){

    Chatroom chatroom = chatroomRepository.findById(chatroomId)
        .orElseThrow(() -> new RingoException("채팅방을 찾을 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));

    if (!isMemberInChatroom(chatroomId, user.getId())){
      log.error("authUserId={}, step=잘못된_유저_요청, status=FAILED", user.getId());
      throw new RingoException("채팅방을 삭제할 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
    }

    chatroomParticipantRepository.deleteAllByChatroom(chatroom);
    messageRepository.deleteAllByChatroomId(chatroomId);
    chatroomRepository.delete(chatroom);
  }

  public boolean isMemberInChatroom(Long roomId, Long userId){
    Chatroom chatroom = chatroomRepository.findById(roomId)
        .orElseThrow(() -> new RingoException("채팅방을 찾을 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));

    // 채팅방 참여자 유저 id 조회
    List<ChatroomParticipant> participants = chatroomParticipantRepository.findAllByChatroom(chatroom);

    List<Long> userIds = new ArrayList<>();

    ChatroomParticipant p1 = participants.get(0);
    ChatroomParticipant p2 = participants.get(1);

    if (!p1.isWithdrawn()){
      userIds.add(p1.getParticipant().getId());
    }

    if (!p2.isWithdrawn()){
      userIds.add(p2.getParticipant().getId());
    }

    return userIds.contains(userId);
  }

  public List<User> findUserInChatroom(Long roomId){
    Chatroom chatroom = chatroomRepository.findById(roomId)
        .orElseThrow(() -> new RingoException("채팅방에 초대된 유저를 찾는 중 채팅방가 존재하지 않습니다", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));
    return chatroomParticipantRepository.findAllByChatroom(chatroom)
        .stream()
        .filter(participant -> !participant.isWithdrawn())
        .map(ChatroomParticipant::getParticipant)
        .toList();
  }

  /**
   * 안읽은 메세지를 읽은 메세지로 전부 변환하는 함수
   */
  @Transactional
  public void changeNotReadToReadMessages(Long roomId, Long userId){
    List<String> messageIdList = messageRepository.findNotReadMessages(roomId, userId)
        .stream().map(Message::getId).toList();
    messageRepository.insertMemberIdInMessage(messageIdList, userId);
  }

  // 약속 잡기
  public GetChatMessageResponseDto saveAppointment(SaveAppointmentRequestDto dto){
    User register = userRepository.findById(dto.registerId()).orElseThrow(
        () -> new RingoException("유저를 찾을 수 없습니다.", ErrorCode.NOT_FOUND_USER, HttpStatus.BAD_REQUEST)
    );
    Chatroom chatroom = chatroomRepository.findById(dto.chatroomId()).orElseThrow(
        () -> new RingoException("채팅방을 찾을 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST)
    );
    if (!isMemberInChatroom(chatroom.getId(), register.getId())){
      throw new RingoException("채팅방 멤버만 약속을 잡을 수 있습니다", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
    }

    Appointment appointment = Appointment.builder()
        .chatroom(chatroom)
        .register(register)
        .place(dto.place())
        .appointmentTime(LocalDateTime.parse(dto.appointmentTime()))
        .alertTime(dto.isAlert() == 1 ? LocalDateTime.parse(dto.alertTime()) : null)
        .isAlert(dto.isAlert() == 1)
        .build();

    Appointment saveAppointment = appointmentRepository.save(appointment);

    return GetChatMessageResponseDto.builder()
        .content("일정이 등록되었어요")
        .appointmentTime(saveAppointment.getAppointmentTime())
        .place(saveAppointment.getPlace())
        .type("APPOINTMENT")
        .build();
  }

  public List<Appointment> getAlertChatrooms(){
    return appointmentRepository.findAllByAlertTimeBeforeAndIsAlert(LocalDateTime.now(), true);
  }

  public void saveAlertOffAppointment(List<Appointment> appointments){
    appointmentRepository.saveAll(appointments);
  }
}
