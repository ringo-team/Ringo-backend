package com.lingo.lingoproject.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Table(name = "CHATROOM_PARTICIPANTS")
@DynamicInsert
public class ChatroomParticipant {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "chatting_room_id")
  private Chatroom chatroom;

  @ManyToOne
  @JoinColumn(name = "participant_user_id")
  private User participant;

  @ColumnDefault(value = "false")
  private Boolean isWithdrawn;
}
