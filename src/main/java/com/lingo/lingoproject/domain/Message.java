package com.lingo.lingoproject.domain;

import com.lingo.lingoproject.utils.Timestamp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "MESSAGES")
public class Message extends Timestamp {
  @Id
  private String id;

  @ManyToOne
  @JoinColumn(name = "chatroom_id")
  private Chatroom chatroom;

  @ManyToOne
  @JoinColumn(name = "user_id")
  private User user;

  @ColumnDefault(value = "false")
  private Boolean isRead;


  @Column(columnDefinition = "TEXT")
  private String content;
}
