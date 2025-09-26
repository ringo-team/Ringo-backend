package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.ChatRoom;
import com.lingo.lingoproject.domain.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessagesRepository extends JpaRepository<Message, String> {

  Page<Message> findAllByChattingRoom(Pageable pageable, ChatRoom chattingRoom);
}
