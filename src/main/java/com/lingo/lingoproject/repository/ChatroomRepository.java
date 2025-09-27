package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.Chatroom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatroomRepository extends JpaRepository<Chatroom, Long> {

}
