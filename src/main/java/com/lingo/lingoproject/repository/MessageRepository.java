package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.Chatroom;
import com.lingo.lingoproject.domain.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, String> {

  Page<Message> findAllByChattingRoom(Chatroom chattingRoom, Pageable pageable);

  void deleteAllByChattingRoom(Chatroom chattingRoom);
}
