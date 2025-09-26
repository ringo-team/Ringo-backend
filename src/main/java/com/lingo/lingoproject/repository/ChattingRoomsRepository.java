package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChattingRoomsRepository extends JpaRepository<ChatRoom, Long> {

}
