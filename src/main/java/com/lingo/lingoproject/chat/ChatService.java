package com.lingo.lingoproject.chat;

import com.lingo.lingoproject.domain.ChattingRoom;
import com.lingo.lingoproject.domain.Messages;
import com.lingo.lingoproject.domain.UserEntity;
import com.lingo.lingoproject.repository.ChattingRoomsRepository;
import com.lingo.lingoproject.repository.MessagesRepository;
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

  private final ChattingRoomsRepository chattingRoomsRepository;
  private final MessagesRepository messagesRepository;
  private final UserRepository userRepository;

  public List<ChatResponseDto> getChattingMessages(Long roomId, int page, int size){
    ChattingRoom chattingRoom = chattingRoomsRepository.findById(roomId)
        .orElseThrow(() -> new IllegalArgumentException("Room not found"));

    Pageable pageable = PageRequest.of(page, size);

    Page<Messages> messages = messagesRepository.findAllByChattingRoom(pageable, chattingRoom);
    List<ChatResponseDto> messageResponses = messages.stream()
        .map(m -> {
          UserEntity user = m.getUser();
          return new ChatResponseDto(user.getId(), user.getNickname(), m.getMessage(), m.getCreatedAt());
        })
        .toList();
    return messageResponses;
  }

  public void saveMessage(ChatResponseDto message, Long roomId){
    ChattingRoom chattingRoom = chattingRoomsRepository.findById(roomId)
        .orElseThrow(() -> new IllegalArgumentException("Room not found"));
    UserEntity user = userRepository.findById(message.userId())
        .orElseThrow(() -> new IllegalArgumentException("User not found"));
    Messages messages = Messages.builder()
        .id(UUID.randomUUID().toString())
        .chattingRoom(chattingRoom)
        .user(user)
        .message(message.content())
        .build();
    messagesRepository.save(messages);
  }
}
