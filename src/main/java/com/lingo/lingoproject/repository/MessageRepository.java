package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.Chatroom;
import com.lingo.lingoproject.domain.Message;
import com.lingo.lingoproject.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, String> {

  Page<Message> findAllByChatroomOrderByCreatedAtDesc(Chatroom chattingRoom, Pageable pageable);

  void deleteAllByChatroom(Chatroom chattingRoom);

  @Query(value = "select count(*) from messages where chatroom_id = :chatroomId and user_id <> :userId "
      + "and is_read = 0 order by created_at desc limit 100", nativeQuery = true)
  int findNumOfNonReadMessages(@Param("chatroomId") Long chatroomId, @Param("userId") Long userId);

  @Query(value = "select * from messages where chatroom_id = :chatroomId and user_id <> :userId "
      + "and is_read = 0 order by created_at", nativeQuery = true)
  List<Message> findNonReadMessages(@Param("chatroomId") Long chatroomId, @Param("userId") Long userId);

  Optional<Message> findFirstByChatroomOrderByCreatedAtDesc(Chatroom chatroom);

  @Modifying
  @Query("update Message m set m.user = null where m.user = :user")
  void disconnectMessageWithUser(User user);
}
