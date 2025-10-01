package com.lingo.lingoproject.domain;

import com.lingo.lingoproject.utils.Timestamp;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "messages")
public class Message extends Timestamp {
  @Id
  private String id;

  @ManyToOne
  @JoinColumn(name = "chatroomId")
  private Chatroom chattingRoom;

  @ManyToOne
  @JoinColumn(name = "userId")
  private User user;

  private String message;
}
