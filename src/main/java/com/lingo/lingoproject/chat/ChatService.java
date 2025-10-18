package com.lingo.lingoproject.chat;

import com.lingo.lingoproject.chat.dto.GetChatResponseDto;
import com.lingo.lingoproject.chat.dto.CreateChatroomDto;
import com.lingo.lingoproject.chat.dto.GetChatroomResponseDto;
import com.lingo.lingoproject.domain.Chatroom;
import com.lingo.lingoproject.domain.ChatroomParticipant;
import com.lingo.lingoproject.domain.Message;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.domain.enums.ChatType;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.repository.ChatroomParticipantRepository;
import com.lingo.lingoproject.repository.ChatroomRepository;
import com.lingo.lingoproject.repository.MessageRepository;
import com.lingo.lingoproject.repository.UserRepository;
import com.lingo.lingoproject.utils.GenericUtils;
import jakarta.transaction.Transactional;
import java.text.SimpleDateFormat;
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

  public List<GetChatResponseDto> getChattingMessages(Long roomId, int page, int size){
    Chatroom chatroom = chatroomRepository.findById(roomId)
        .orElseThrow(() -> new RingoException("id에 해당하는 채팅방이 존재하지 않습니다.", HttpStatus.BAD_REQUEST));

    Pageable pageable = PageRequest.of(page, size);

    Page<Message> messages = messageRepository.findAllByChatroomOrderByCreatedAtDesc(chatroom, pageable);
    List<GetChatResponseDto> messageResponses = messages.stream()
        .map(m -> {
          User user = m.getUser();
          log.info(m.getCreatedAt().toString());
          return GetChatResponseDto.builder()
              .userId(user.getId())
              .content(m.getContent())
              .createdAt(m.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
              .isRead(true)
              .build();
        })
        .toList();
    return messageResponses;
  }

  public void saveMessage(GetChatResponseDto message, Long roomId){
    Chatroom chatroom = chatroomRepository.findById(roomId)
        .orElseThrow(() -> new RingoException("id에 해당하는 채팅방을 찾을 수 없습니다..", HttpStatus.BAD_REQUEST));
    User user = userRepository.findById(message.getUserId())
        .orElseThrow(() -> new RingoException("id에 해당하는 유저를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST));
    Message messages = Message.builder()
        .id(UUID.randomUUID().toString())
        .chatroom(chatroom)
        .user(user)
        .content(message.getContent())
        .isRead(message.getIsRead())
        .build();
    messageRepository.save(messages);
  }

  public Chatroom createChatroom(CreateChatroomDto dto){
    if(!genericUtils.isContains(ChatType.values(), dto.chatType())){
      throw new RingoException("적절하지 않은 채팅타입이 입력되었습니다.", HttpStatus.BAD_REQUEST);
    }
    User user1 = userRepository.findById(dto.user1Id())
        .orElseThrow(() -> new RingoException("id에 해당하는 유저를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST));
    User user2 = userRepository.findById(dto.user2Id())
        .orElseThrow(() -> new RingoException("id에 해당하는 유저를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST));
    Chatroom chatroom = Chatroom.builder()
        .chatroomName(dto.user1Id().toString() +"_" +dto.user2Id().toString())
        .type(ChatType.valueOf(dto.chatType()))
        .build();
    Chatroom savedChatroom = chatroomRepository.save(chatroom);
    ChatroomParticipant participant1 = ChatroomParticipant.builder()
        .participant(user1)
        .chatroom(chatroom)
        .build();
    ChatroomParticipant participant2 = ChatroomParticipant.builder()
        .participant(user2)
        .chatroom(chatroom)
        .build();
    chatroomParticipantRepository.saveAll(List.of(participant1, participant2));
    return savedChatroom;
  }

  public List<GetChatroomResponseDto> getAllChatroomByUserId(Long userId){
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RingoException("id에 해당하는 유저를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST));
    List<Chatroom> chatrooms = chatroomRepository.findAllByUser(user);
    List<GetChatroomResponseDto> list = new ArrayList<>();
    for(Chatroom chatroom : chatrooms){
      // 채팅 상대방을 찾는다.
      ChatroomParticipant participant = chatroomParticipantRepository.findByParticipantNotAndChatroom(user, chatroom);
      /**
       * 회원 탈퇴의 경우 participant.getParticipant가 null일 수 있다.
       */
      String nickname = null;
      if (participant == null){
        nickname = "알 수 없음";
      }else{
        nickname = participant.getParticipant().getNickname();
      }
      // 읽지 않은 대화 개수를 찾는다.
      int numOfNonReadMessages = messageRepository.findNumOfNonReadMessages(chatroom.getId(), user.getId());
      Optional<Message> lastMessage = messageRepository.findFirstByChatroomOrderByCreatedAtDesc(chatroom);
      list.add(GetChatroomResponseDto.builder()
              .chatroomId(chatroom.getId())
              .chatroomName(nickname)
              .lastChatMessage(lastMessage.map(Message::getContent).orElse(null))
              .NumOfNonReadMessages(numOfNonReadMessages)
          .build());
    }
    return list;
  }

  @Transactional
  public void deleteChatroom(Long id){
    Chatroom chatroom = chatroomRepository.findById(id)
        .orElseThrow(() -> new RingoException("chatroom not found", HttpStatus.BAD_REQUEST));
    chatroomParticipantRepository.deleteAllByChatroom(chatroom);
    messageRepository.deleteAllByChatroom(chatroom);
    chatroomRepository.delete(chatroom);
  }

  public boolean isMemberInChatroom(Long roomId, Long userId){
    Chatroom chatroom = chatroomRepository.findById(roomId)
        .orElseThrow(() -> new RingoException("Room not found", HttpStatus.BAD_REQUEST));
    List<Long> userIds = chatroomParticipantRepository.findAllByChatroom(chatroom)
        .stream()
        .map(ChatroomParticipant::getParticipant)
        .map(User::getId)
        .toList();
    return userIds.contains(userId);
  }

  public List<String> getUsernamesInChatroom(Long roomId){
    Chatroom chatroom = chatroomRepository.findById(roomId)
        .orElseThrow(() -> new RingoException("Room not found", HttpStatus.BAD_REQUEST));
    return chatroomParticipantRepository.findAllByChatroom(chatroom)
        .stream()
        .map(ChatroomParticipant::getParticipant)
        .map(User::getUsername)
        .toList();
  }

  /**
   * 안읽은 메세지를 읽은 메세지로 전부 변환하는 함수
   */
  public void changeNonReadTpReadMessages(Long roomId, Long userId){
    List<Message> messages = messageRepository.findNonReadMessages(roomId, userId);
    messages.forEach( message -> message.setIsRead(true));
    messageRepository.saveAll(messages);
  }
}
