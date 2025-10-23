package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.Chatroom;
import com.lingo.lingoproject.domain.ChatroomParticipant;
import com.lingo.lingoproject.domain.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatroomParticipantRepository extends JpaRepository<ChatroomParticipant, Long> {

  List<ChatroomParticipant> findAllByChatroom(Chatroom chatroom);

  void deleteAllByChatroom(Chatroom chatroom);


  @Modifying
  @Query("update ChatroomParticipant cp set cp.participant = null, cp.isWithdrawn = true where cp.participant = :user")
  void disconnectChatroomParticipantWithUser(@Param("user") User user);
}
