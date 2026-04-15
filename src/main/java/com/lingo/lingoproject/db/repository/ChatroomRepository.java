package com.lingo.lingoproject.db.repository;

import com.lingo.lingoproject.db.domain.Chatroom;
import com.lingo.lingoproject.db.domain.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatroomRepository extends JpaRepository<Chatroom, Long> {
  @Query("select cp.chatroom from ChatroomParticipant cp where cp.participant = :user")
  List<Chatroom> findAllByUser(@Param("user") User user);
}
