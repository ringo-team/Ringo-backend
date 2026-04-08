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
import com.lingo.lingoproject.domain.Matching;
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

  /**
   * user가 속한 특정 채팅방의 메세지를 페이지 단위로 조회하는 메소드
   *
   * @param user API 요청한 유저 객체
   * @param chatroomId 메세지 확인할 채팅방의 Id
   * @param page 조회할 페이지 번호
   * @param size 조회할 페이지 크기
   * @return 해당 채팅방의 메세지를 포함한 정보들을 반환
   *
   */
  public GetChatResponseDto getChatMessages(User user, Long chatroomId, int page, int size){

    if (!isMemberInChatroom(chatroomId, user.getId())){
      throw new RingoException("요청한 유저와 채팅방 멤버 id가 서로 다릅니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
    }

    Chatroom chatroom = chatroomRepository.findById(chatroomId)
        .orElseThrow(() -> new RingoException("채팅방을 찾을 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));

    // 메세지 조회
    List<GetChatMessageResponseDto> messagesDto = getChatMessagesDto(chatroomId, page, size);

    // 채팅방 멤버 조회
    List<ChatroomParticipant> participants = chatroomParticipantRepository.findAllByChatroom(chatroom);

    if (!isMemberInChatroom(chatroomId, user.getId())){
      throw new RingoException("채팅방에 소속되지 않은 유저이거나 탈퇴된 유저입니다.", ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST);
    }
    if (participants.size() == 2 && (
            participants.get(0).isWithdrawn() ||
            participants.get(1).isWithdrawn()
        ))  return handleWithdrawnUserChatroom(messagesDto, user);


    // 상대방 유저 확인
    User first = participants.get(0).getParticipant();
    User second = participants.get(1).getParticipant();
    User otherChatroomMember = first.getId().equals(user.getId()) ? second : first;

    messagesDto.forEach(dto -> markIsReadFlag(dto, user));

    List<String> hashtags = hashtagRepository.findAllByUser(otherChatroomMember).stream()
        .map(Hashtag::getHashtag).toList();
    GetChatroomMemberInfoResponseDto memberInfo = GetChatroomMemberInfoResponseDto.builder()
        .profileUrl(otherChatroomMember.getProfile().getImageUrl())
        .userId(otherChatroomMember.getId())
        .nickname(otherChatroomMember.getNickname())
        .hashtag(hashtags)
        .build();
    log.info("""
        
        채팅방_id : {},
        API_user_id : {},
        채팅_상대방_유저 : {
          id: {}.
          nickname: {},
          hashtag: {}
        },
        
        """,
        chatroomId,
        user.getId(),
        otherChatroomMember.getId(),
        otherChatroomMember.getNickname(),
        hashtags
        );
    return GetChatResponseDto.builder()
        .result(ErrorCode.SUCCESS.getCode())
        .memberInfo(memberInfo)
        .messages(messagesDto)
        .build();
  }

  /**
   * 채팅 상대방이 회원탈퇴한 채팅방을 다루는 함수
   * @param messagesDto 순수 메세지 객체 dto
   * @param user 메세지 조회 요청 유저 객체
   * @return GetChatResponseDto
   */
  private GetChatResponseDto handleWithdrawnUserChatroom(
      List<GetChatMessageResponseDto> messagesDto,
      User user
  ){
    messagesDto.forEach(dto -> markIsReadFlag(dto, user));

    GetChatroomMemberInfoResponseDto memberInfo = GetChatroomMemberInfoResponseDto.builder()
        .profileUrl(null)
        .userId(null)
        .nickname("알 수 없음")
        .hashtag(null)
        .build();
    return GetChatResponseDto.builder()
        .memberInfo(memberInfo)
        .messages(messagesDto)
        .result(ErrorCode.SUCCESS.getCode())
        .build();
  }

  private List<GetChatMessageResponseDto> getChatMessagesDto(Long chatroomId, int page, int size){
    // 페이지네이션
    Pageable pageable = PageRequest.of(page, size);

    Page<Message> messages =  messageRepository.findAllByChatroomIdOrderByCreatedAtDesc(chatroomId, pageable);
    return messages.stream()
        .map(m -> GetChatMessageResponseDto.from(chatroomId, m))
        .toList();
  }

  private void markIsReadFlag(GetChatMessageResponseDto dto, User chatUser){
    List<Long> readerIds = dto.getReaderIds();

    if (readerIds.contains(dto.getSenderId())){
      log.info("""
          
          readerIds: {},
          보낸_사람_id: {},
          readerIds에 보낸 사람의 id가 존재하지 않습니다.
          
          """, readerIds, dto.getSenderId());
      throw new RingoException("readerIds에 보낸 사람의 id가 존재하지 않습니다.", ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST);
    }

    if (readerIds.isEmpty()) {
      log.error("채팅을 읽은 사람이 없습니다.");
      dto.setIsRead(0); // 읽은 사람이 없는 경우
    }
    /*
     * 보낸 사람만 읽은 경우 -> 안 읽음 처리
     * readerIds == 1 && 보낸사람과 조회요청한 사람이 다른 경우 -> 있을 수 없음
     */
    else if (
            dto.getReaderIds().size() == 1 &&
            dto.getSenderId().equals(chatUser.getId())
    ) dto.setIsRead(0);
    else dto.setIsRead(1);
  }

  /**
   * roomId 의 채팅방에 접속한 유저들의 id 리스트를 조회
   *
   * @param roomMembers roomId의 채팅방에 포함된 유저 객체들
   * @param roomId 조회하려는 채팅방_id
   * @return 채팅방에 접속한 유저들의 id 리스트
   */
  public List<Long> getConnectedUserIdList(List<User> roomMembers, Long roomId) {

    try {

      List<Long> result = new ArrayList<>();

      for (User member : roomMembers){
        String key = "connect::" + member.getId() + "::" + roomId;

        if (redisTemplate.hasKey(key)) result.add(member.getId());
      }

      log.info("""
          
          채팅방_id : {}, 에 접속한 유저 리스트: {}
          
          """, roomId, result);

      return result;

    }catch (Exception e){
      log.error("""

          chatroomId={},
          step=메세제_저장,
          status=FAILED

          """, roomId, e);
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
    log.error("""

        chatroomId={},
        userLoginId={},
        step=메세지_전송_실패,
        status=FAILED

        """, roomId, userLoginId,
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
      logErrorByMatchingStatusIssue(user1, user2);
      throw new RingoException("매칭되지 않은 쌍은 채팅방을 생성할 수 없습니다.", ErrorCode.NO_AUTH, HttpStatus.BAD_REQUEST);
    }

    // 채팅방 생성 및 저장
    Chatroom chatroom = Chatroom.builder()
        .chatroomName(dto.user1Id() + "_" +dto.user2Id())
        .type(chatType)
        .build();
    Chatroom savedChatroom = chatroomRepository.save(chatroom);

    log.info("""
        
        유저1_id: {} | 닉네임: {} | 성별: {} | 나이: {}
        유저2_id: {} | 닉네임: {} | 성별: {} | 나이: {}
        채팅_타입: {},
        채팅_생성_시간: {},
        
        """
    , user1.getId(), user1.getNickname(), user1.getGender(), user1.getAge()
    , user2.getId(), user2.getNickname(), user2.getGender(), user2.getAge()
    , savedChatroom.getType(), savedChatroom.getCreatedDate());

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

  private void logErrorByMatchingStatusIssue(User user1, User user2){
    Matching matching1 = matchingRepository.findFirstByRequestUserAndRequestedUser(user1, user2);
    Matching matching2 = matchingRepository.findFirstByRequestUserAndRequestedUser(user2, user1);
    if (matching1 == null && matching2 == null){
      log.error("""
            
            user id: {}, {},
            서로 매칭이 이루어진적이 없습니다.
            
            """, user1.getId(), user2.getId());
    }
    else if (matching1 == null){
      MatchingStatus status = matching2.getMatchingStatus();
      if (status != null) {
        logCreateChatroomErrorLog(user2, user1, status,
            "요청은 있지만 matching_status가 아직 ACCEPTED 되지 않음");
      }
      logCreateChatroomErrorLog(user2, user1, null, "matching_status가 null 임");
    }
    else if (matching2 == null){
      MatchingStatus status = matching1.getMatchingStatus();
      if (status != null) {
        logCreateChatroomErrorLog(user1, user2, status,
            "요청은 있지만 matching_status가 아직 ACCEPTED 되지 않음");
      }
      logCreateChatroomErrorLog(user1, user2, null, "matching_status가 null 임");
    }
    else {
      log.error("둘다 요청을 하였지만 matching_status가 ACCEPTED된 요청이 없음");
      logCreateChatroomErrorLog(user1, user2, matching1.getMatchingStatus(), "matching_status가 아직 ACCEPTED되지 않음");
      logCreateChatroomErrorLog(user2, user1, matching1.getMatchingStatus(), "matching_status가 아직 ACCEPTED되지 않음");
    }
  }

  private void logCreateChatroomErrorLog(
      User requestUser,
      User requestedUser,
      MatchingStatus status,
      String message
  ){
    log.error("""
              
              request_user_id: {},
              requested_user_id: {},
              matching_status: {},
              message: {}
              
              """,
        requestUser.getId(),
        requestedUser.getId(),
        status, message);
  }

  public List<GetChatroomResponseDto> getAllChatroomByUserId(User user){
    // 채팅방 조회
    List<Chatroom> chatrooms = chatroomRepository.findAllByUser(user);

    List<GetChatroomResponseDto> responseDtoList = new ArrayList<>();

    for(Chatroom chatroom : chatrooms){

      // 채팅 상대방 유저 닉네임 조회
      List<ChatroomParticipant> participants = chatroomParticipantRepository.findAllByChatroom(chatroom);

      if (participants == null || participants.isEmpty()) {
        // 채팅방은 존재하는데 채팅참여자가 없음
        // 빈채팅방 조회됨
        // 로그 남기고 계속 남은 채팅방 조회
        log.error("""
            
            채팅방_id: {}, 채팅방은 존재하지만 참여자가 없습니다.
            
            """, chatroom.getId());
        continue;
      }

      String nickname = null;
      String imageUrl = null;

      ChatroomParticipant p1 = participants.get(0);
      ChatroomParticipant p2 = participants.get(1);

      if (!isMemberInChatroom(chatroom.getId(), user.getId())){
        throw new RingoException("채팅방에 소속되지 않은 유저이거나 탈퇴된 유저입니다.", ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST);
      }

      if (p1.isWithdrawn() || p2.isWithdrawn()){
        log.info("""
            
            p1_탈퇴여부: {} | p2_탈퇴여부: {}
            회원 탈퇴한 회원의 채팅에 접속하였습니다.
            
            """, p1.isWithdrawn(), p2.isWithdrawn()
        );
        nickname = "알 수 없음";
      }
      else{
        User p1User = p1.getParticipant();
        User p2User = p2.getParticipant();
        boolean isActive = user.getId().equals(p1User.getId()) ? p1.isActive() : p2.isActive();
        if (!isActive) {
          log.info("""
              
              userId: {},
              채팅방_id: {}
              채팅방이 조회되지 않습니다
              
              """, user.getId(), chatroom.getId());
          continue;
        }
        User chatOpponent = user.getId().equals(p1User.getId()) ? p2User : p1User;
        nickname = chatOpponent.getNickname();
        imageUrl = chatOpponent.getProfile().getImageUrl();
        log.info("""
            
            유저1_id: {} | 닉네임: {} | 성별: {} | 생년월일: {}
            유저2_id: {} | 닉네임: {} | 성별: {} | 생년월일: {}
            
            요청자_id: {} | 채팅_상대방_id: {}
            """, p1User.getId(), p1User.getNickname(), p1User.getGender(), p1User.getBirthday(),
            p2User.getId(), p2User.getNickname(), p2User.getGender(), p2User.getBirthday(),
            user.getId(), chatOpponent.getId());
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

      GetChatroomResponseDto response = GetChatroomResponseDto.builder()
          .chatroomId(chatroom.getId())
          .chatOpponent(nickname)
          .chatOpponentProfileUrl(imageUrl)
          .lastChatMessage(lastMessage.map(Message::getContent).orElse(null))
          .numberOfNotReadMessages(numberOfNotReadMessages)
          .lastSendDateTime(lastSendDateTime)
          .chatroomSize(2)
          .build();

      log.info("""
          
          채팅방_id: {},
          채팅_상대방_닉네임: {},
          읽기_않은_메세지_개수: {},
          마지막_메세지_전송_시간: {}
          
          """,
          response.chatroomId(),
          response.chatOpponent(),
          response.numberOfNotReadMessages(),
          response.lastSendDateTime()
          );

      responseDtoList.add(response);
    }
    log.info("""
        
        조회된 채팅방의 개수: {}
        
        """, responseDtoList.size());
    return responseDtoList;
  }

  @Transactional
  public void deleteChatroom(Long chatroomId, User user){

    Chatroom chatroom = chatroomRepository.findById(chatroomId)
        .orElseThrow(() -> new RingoException("채팅방을 찾을 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));

    if (!isMemberInChatroom(chatroomId, user.getId())){
      log.error("""

          authUserId={},
          step=잘못된_유저_요청,
          status=FAILED

          """, user.getId());
      throw new RingoException("채팅방을 삭제할 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
    }

    chatroomParticipantRepository.deleteAllByChatroom(chatroom);
    messageRepository.deleteAllByChatroomId(chatroomId);
    chatroomRepository.delete(chatroom);
  }

  public boolean isMemberInChatroom(Long roomId, Long userId){

    List<Long> userIds = getParticipantsInChatroom(roomId).stream().map(User::getId).toList();

    if (userIds.isEmpty()){
      log.error("""
          
          요청자_id: {} | 채팅방_크기: {}
          모두 회원탈퇴한 채팅방에 조회를 요청하였습니다.
          
          """, userId, 0);
    }
    else if (userIds.size() == 1){
      log.info("""
          
          요청자_id: {} | 채팅_참여자_id: {} | 채팅방_크기: {}
          한명이 회원탈퇴한 채팅방에 조회를 요청하였습니다.
          * 확인: 채팅방 참여자와 요청자가 동일한 사용자인지 확인해야합니다.
          
          """, userId, userIds.getFirst(), 1);
    }
    else {
      log.info("""
          
          요청자_id: {} | 채팅_참여자_id: {}, {} | 채팅방_크기: {}
          한명이 회원탈퇴한 채팅방에 조회를 요청하였습니다.
          * 확인: 채팅방 참여자와 요청자가 동일한 사용자인지 확인해야합니다.
          
          """, userId, userIds.get(0), userIds.get(1), 1);
    }

    return userIds.contains(userId);
  }

  public List<User> getParticipantsInChatroom(Long roomId){
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
  public void readAllMessages(Long roomId, Long userId){
    messageRepository.readAllMessages(roomId, userId);
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

    log.info("""
        
        ########## 약속잡기 ##########
        채팅방_id: {},
        등록자_닉네임: {},
        장소: {},
        약속시간: {},
        알림_여부: {},
        알림_시간: {}
        #############################
        
        """, chatroom.getId(),
        register.getNickname(),
        dto.place(),
        dto.appointmentTime(),
        dto.isAlert() == 1,
        dto.alertTime()
        );

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

  public List<Appointment> getScheduledAppointments(){
    return appointmentRepository.findAllByAlertTimeBeforeAndIsAlert(LocalDateTime.now(), true);
  }

  public void saveAppointment(List<Appointment> appointments){
    appointmentRepository.saveAll(appointments);
  }
}
