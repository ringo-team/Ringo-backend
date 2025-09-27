package com.lingo.lingoproject.chat;

import com.lingo.lingoproject.chat.dto.CreateChatroomDto;
import com.lingo.lingoproject.domain.Chatroom;
import com.lingo.lingoproject.domain.ChatroomParticipant;
import com.lingo.lingoproject.domain.Message;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.repository.ChatroomParticipantRepository;
import com.lingo.lingoproject.repository.ChatroomRepository;
import com.lingo.lingoproject.repository.MessageRepository;
import com.lingo.lingoproject.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService {

  private final ChatroomRepository chatroomRepository;
  private final MessageRepository messageRepository;
  private final UserRepository userRepository;
  private final ChatroomParticipantRepository chatroomParticipantRepository;

  public List<ChatResponseDto> getChattingMessages(Long roomId, int page, int size){
    Chatroom chatroom = chatroomRepository.findById(roomId)
        .orElseThrow(() -> new IllegalArgumentException("Room not found"));

    Pageable pageable = PageRequest.of(page, size);

    Page<Message> messages = messageRepository.findAllByChattingRoom(chatroom, pageable);
    List<ChatResponseDto> messageResponses = messages.stream()
        .map(m -> {
          User user = m.getUser();
          return new ChatResponseDto(user.getId(), user.getNickname(), m.getMessage(), m.getCreatedAt());
        })
        .toList();
    return messageResponses;
  }

  public void saveMessage(ChatResponseDto message, Long roomId){
    Chatroom chatroom = chatroomRepository.findById(roomId)
        .orElseThrow(() -> new IllegalArgumentException("Room not found"));
    User user = userRepository.findById(message.userId())
        .orElseThrow(() -> new IllegalArgumentException("User not found"));
    Message messages = Message.builder()
        .id(UUID.randomUUID().toString())
        .chattingRoom(chatroom)
        .user(user)
        .message(message.content())
        .build();
    messageRepository.save(messages);
  }

  public Chatroom createChatroom(CreateChatroomDto dto){
    User user1 = userRepository.findById(dto.user1Id())
        .orElseThrow(() -> new IllegalArgumentException("User not found"));
    User user2 = userRepository.findById(dto.user2Id())
        .orElseThrow(() -> new IllegalArgumentException("User not found"));
    Chatroom chatroom = Chatroom.builder()
        .chatroomName(dto.user1Id().toString() +"_" +dto.user2Id().toString())
        .type(dto.chatType())
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

  public void deleteChatroom(Long id){
    Chatroom chatroom = chatroomRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("chatroom not found"));
    chatroomParticipantRepository.deleteAllByChatroom(chatroom);
    messageRepository.deleteAllByChattingRoom(chatroom);
    chatroomRepository.delete(chatroom);
  }
}
