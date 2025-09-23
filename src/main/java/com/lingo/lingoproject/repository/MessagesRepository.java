package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.ChattingRoom;
import com.lingo.lingoproject.domain.Messages;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessagesRepository extends JpaRepository<Messages, String> {

  Page<Messages> findAllByChattingRoom(Pageable pageable, ChattingRoom chattingRoom);
}
