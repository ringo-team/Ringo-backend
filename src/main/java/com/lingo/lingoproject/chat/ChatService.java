package com.lingo.lingoproject.chat;


import com.lingo.lingoproject.chat.dto.GetChatMessageResponseDto;
import com.lingo.lingoproject.chat.dto.CreateChatroomRequestDto;
import com.lingo.lingoproject.chat.dto.GetChatResponseDto;
import com.lingo.lingoproject.chat.dto.GetChatroomMemberInfoResponseDto;
import com.lingo.lingoproject.chat.dto.GetChatroomResponseDto;
import com.lingo.lingoproject.domain.Chatroom;
import com.lingo.lingoproject.domain.ChatroomParticipant;
import com.lingo.lingoproject.domain.FailedChatMessageLog;
import com.lingo.lingoproject.domain.Hashtag;
import com.lingo.lingoproject.domain.Message;
import com.lingo.lingoproject.domain.Profile;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.domain.enums.ChatType;
import com.lingo.lingoproject.domain.enums.MatchingStatus;
import com.lingo.lingoproject.exception.ErrorCode;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.repository.ChatroomParticipantRepository;
import com.lingo.lingoproject.repository.ChatroomRepository;
import com.lingo.lingoproject.mongo_repository.MessageRepository;
import com.lingo.lingoproject.repository.FailedChatMessageLogRepository;
import com.lingo.lingoproject.repository.HashtagRepository;
import com.lingo.lingoproject.repository.MatchingRepository;
import com.lingo.lingoproject.repository.ProfileRepository;
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
import org.springframework.data.redis.core.ValueOperations;
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
  private final GenericUtils genericUtils;
  private final MatchingRepository matchingRepository;
  private final RedisTemplate<String, Object> redisTemplate;
  private final ProfileRepository profileRepository;
  private final HashtagRepository hashtagRepository;
  private final FailedChatMessageLogRepository failedChatMessageLogRepository;


  public GetChatResponseDto getChatMessages(User user, Long chatroomId, int page, int size){
    // 페이지네이션
    Pageable pageable = PageRequest.of(page, size);

    // 메세지 조회
    Page<Message> messages = messageRepository.findAllByChatroomIdOrderByCreatedAtDesc(chatroomId, pageable);
    List<GetChatMessageResponseDto> messagesDto = messages.stream()
        .map(m -> GetChatMessageResponseDto.from(chatroomId, m))
        .toList();

    // 채팅방 및 채팅방 멤버 조회
    Chatroom chatroom = chatroomRepository.findById(chatroomId)
        .orElseThrow(() -> new RingoException("채팅방을 찾을 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));
    List<ChatroomParticipant> participants = chatroomParticipantRepository.findAllByChatroom(chatroom);
    if (participants.size() == 1){
      return GetChatResponseDto.builder()
          .result(ErrorCode.SUCCESS.getCode())
          .memberInfo(null)
          .messages(messagesDto)
          .build();
    }

    // 상대방 유저 확인
    User firstParticipant = participants.getFirst().getParticipant();
    User otherChatroomMember;
    if (firstParticipant.getId().equals(user.getId())){
      otherChatroomMember = participants.get(1).getParticipant();
    }
    else otherChatroomMember = participants.get(0).getParticipant();

    // 상대방 유저 프로필 조회
    Profile userProfile = profileRepository.findByUser(otherChatroomMember).orElse(null);
    List<String> hashtags = hashtagRepository.findAllByUser(otherChatroomMember).stream()
        .map(Hashtag::getHashtag).toList();
    GetChatroomMemberInfoResponseDto memberInfo = GetChatroomMemberInfoResponseDto.builder()
        .profileUrl(userProfile != null ? userProfile.getImageUrl() : null)
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

  public Boolean existReceiverInChatroom(Long senderId, Long receiverId, Long roomId) {

    try {
      ValueOperations<String, Object> ops = redisTemplate.opsForValue();

      return ops.getOperations().hasKey("connect::" + receiverId + "::" + roomId);

    }catch (Exception e){
      log.error("senderId={}, chatroomId={}, step=메세제_저장, status=FAILED",  senderId, roomId, e);
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
    messageRepository.delete(savedMessage);
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
    if(!genericUtils.isContains(ChatType.values(), dto.chatType())){
      throw new RingoException("적절하지 않은 채팅타입이 입력되었습니다.", ErrorCode.BAD_PARAMETER, HttpStatus.BAD_REQUEST);
    }

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
        .chatroomName(dto.user1Id().toString() + "_" +dto.user2Id().toString())
        .type(ChatType.valueOf(dto.chatType()))
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

      // 채팅방 참여자 닉네임 조회
      List<String> roomMemberNicknames = chatroomParticipantRepository
          .findAllByChatroom(chatroom)
          .stream()
          .map(chatroomParticipant -> {

            // 참여자가 앱을 탈퇴했으면 "알 수 없음" 으로 변경
            if(chatroomParticipant.getIsWithdrawn()) return "알 수 없음";

            // 그 외의 경우 닉네임 조회
            else return chatroomParticipant.getParticipant().getNickname();
          })
          .toList();

      // 유저가 읽지 않은 메세지의 개수를 조회한다.
      int numberOfNotReadMessages = messageRepository.findNumberOfNotReadMessages(chatroom.getId(),
          user.getId());

      // 채팅방 마지막 메세지를 조회한다.
      Optional<Message> lastMessage = messageRepository.findFirstByChatroomIdOrderByCreatedAtDesc(
          chatroom.getId());

      responseDtoList.add(GetChatroomResponseDto.builder()
          .chatroomId(chatroom.getId())
          .participants(roomMemberNicknames)
          .lastChatMessage(lastMessage.map(Message::getContent).orElse(null))
          .NumberOfNotReadMessages(numberOfNotReadMessages)
          .chatroomSize(roomMemberNicknames.size())
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
    List<Long> userIds = chatroomParticipantRepository.findAllByChatroom(chatroom)
        .stream()
        .filter(cp -> !cp.getIsWithdrawn())
        .map(ChatroomParticipant::getParticipant)
        .map(User::getId)
        .toList();

    return userIds.contains(userId);
  }

  public List<User> findUserInChatroom(Long roomId){
    Chatroom chatroom = chatroomRepository.findById(roomId)
        .orElseThrow(() -> new RingoException("채팅방에 초대된 유저를 찾는 중 채팅방가 존재하지 않습니다", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));
    return chatroomParticipantRepository.findAllByChatroom(chatroom)
        .stream()
        .filter(participant -> !participant.getIsWithdrawn())
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
}
