package com.lingo.lingoproject.domain;

import com.lingo.lingoproject.utils.Timestamp;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Messages extends Timestamp {
  @Id
  private String id;

  @ManyToOne
  @JoinColumn(name = "chatroomId")
  private ChattingRoom chattingRoom;

  @ManyToOne
  @JoinColumn(name = "userId")
  private UserEntity user;

  private String message;
}
