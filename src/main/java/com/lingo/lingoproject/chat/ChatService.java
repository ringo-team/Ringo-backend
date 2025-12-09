package com.lingo.lingoproject.chat;

import com.lingo.lingoproject.chat.dto.GetChatResponseDto;
import com.lingo.lingoproject.chat.dto.CreateChatroomRequestDto;
import com.lingo.lingoproject.chat.dto.GetChatroomResponseDto;
import com.lingo.lingoproject.domain.Chatroom;
import com.lingo.lingoproject.domain.ChatroomParticipant;
import com.lingo.lingoproject.domain.Message;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.domain.enums.ChatType;
import com.lingo.lingoproject.domain.enums.MatchingStatus;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.repository.ChatroomParticipantRepository;
import com.lingo.lingoproject.repository.ChatroomRepository;
import com.lingo.lingoproject.mongo_repository.MessageRepository;
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

  public List<GetChatResponseDto> getChatMessages(Long chatroomId, int page, int size){
    // 페이지네이션
    Pageable pageable = PageRequest.of(page, size);

    // 메세지 조회
    Page<Message> messages = messageRepository.findAllByChatroomIdOrderByCreatedAtDesc(chatroomId, pageable);

    return messages.stream()
        .map(m ->
          GetChatResponseDto.builder()
              .chatroomId(chatroomId)
              .senderId(m.getSenderId())
              .content(m.getContent())
              .createdAt(m.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
              .readerIds(m.getReaderIds())
              .build()
        )
        .toList();
  }

  public Message saveMessage(GetChatResponseDto message, Long chatroomId){
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
      throw new RingoException("적절하지 않은 채팅타입이 입력되었습니다.", HttpStatus.BAD_REQUEST);
    }

    // 유저 조회
    User user1 = userRepository.findById(dto.user1Id())
        .orElseThrow(() -> new RingoException("id에 해당하는 유저를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST));
    User user2 = userRepository.findById(dto.user2Id())
        .orElseThrow(() -> new RingoException("id에 해당하는 유저를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST));

    // 매칭된 유저가 아니면 채팅방을 생성할 수 없다.
    if (!(matchingRepository.existsByRequestUserAndRequestedUserAndMatchingStatus(user1, user2, MatchingStatus.ACCEPTED)
        || matchingRepository.existsByRequestUserAndRequestedUserAndMatchingStatus(user2, user1, MatchingStatus.ACCEPTED))
    ){
      throw new RingoException("매칭되지 않은 쌍은 채팅방을 생성할 수 없습니다.", HttpStatus.BAD_REQUEST);
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
      List<String> participants = chatroomParticipantRepository
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
          .participants(participants)
          .lastChatMessage(lastMessage.map(Message::getContent).orElse(null))
          .NumberOfNotReadMessages(numberOfNotReadMessages)
          .chatroomSize(participants.size())
          .build());
    }
    return responseDtoList;
  }

  @Transactional
  public void deleteChatroom(Long chatroomId, User user){

    Chatroom chatroom = chatroomRepository.findById(chatroomId)
        .orElseThrow(() -> new RingoException("채팅방을 찾을 수 없습니다.", HttpStatus.BAD_REQUEST));

    if (!isMemberInChatroom(chatroomId, user.getId())){
      log.error("authUserId={}, step=잘못된_유저_요청, status=FAILED", user.getId());
      throw new RingoException("채팅방을 삭제할 권한이 없습니다.", HttpStatus.FORBIDDEN);
    }

    chatroomParticipantRepository.deleteAllByChatroom(chatroom);
    messageRepository.deleteAllByChatroomId(chatroomId);
    chatroomRepository.delete(chatroom);
  }

  public boolean isMemberInChatroom(Long roomId, Long userId){
    Chatroom chatroom = chatroomRepository.findById(roomId)
        .orElseThrow(() -> new RingoException("채팅방을 찾을 수 없습니다.", HttpStatus.BAD_REQUEST));

    // 채팅방 참여자 유저 id 조회
    List<Long> userIds = chatroomParticipantRepository.findAllByChatroom(chatroom)
        .stream()
        .filter(cp -> !cp.getIsWithdrawn())
        .map(ChatroomParticipant::getParticipant)
        .map(User::getId)
        .toList();

    return userIds.contains(userId);
  }


  /**
   * 채팅방에 존재하는 유저들의 이메일들을 조회하는 함수
   */
  public List<String> getUserEmailsInChatroom(Long roomId){
    Chatroom chatroom = chatroomRepository.findById(roomId)
        .orElseThrow(() -> new RingoException("채팅방에 초대된 유저를 찾는 중 채팅방가 존재하지 않습니다", HttpStatus.BAD_REQUEST));

    return chatroomParticipantRepository.findAllByChatroom(chatroom)
        .stream()
        .filter(participant -> !participant.getIsWithdrawn())
        .map(ChatroomParticipant::getParticipant)
        .map(User::getUsername)
        .toList();
  }

  /**
   * 안읽은 메세지를 읽은 메세지로 전부 변환하는 함수
   */
  public void changeNotReadToReadMessages(Long roomId, Long userId){
    List<Message> messages = messageRepository.findNotReadMessages(roomId, userId);
    messages.forEach( message -> {
      List<Long> readerIds = message.getReaderIds();
      if(!readerIds.contains(userId)){
        readerIds.add(userId);
      }
    });
    messageRepository.saveAll(messages);
  }
}
