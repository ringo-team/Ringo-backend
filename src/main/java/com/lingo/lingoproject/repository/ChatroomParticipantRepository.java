package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.Chatroom;
import com.lingo.lingoproject.domain.ChatroomParticipant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatroomParticipantRepository extends JpaRepository<ChatroomParticipant, Long> {

  public List<ChatroomParticipant> findAllByChatroom(Chatroom chatroom);

  void deleteAllByChatroom(Chatroom chatroom);
}
