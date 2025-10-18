package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.Chatroom;
import com.lingo.lingoproject.domain.ChatroomParticipant;
import com.lingo.lingoproject.domain.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatroomRepository extends JpaRepository<Chatroom, Long> {
  @Query("select c from Chatroom c join ChatroomParticipant cp on c = cp.chatroom where cp.participant = :user")
  List<Chatroom> findAllByUser(@Param("user") User user);
}
