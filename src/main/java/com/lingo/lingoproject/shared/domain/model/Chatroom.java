package com.lingo.lingoproject.shared.domain.model;

import com.lingo.lingoproject.shared.domain.model.ChatType;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.http.HttpStatus;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Table(name = "CHATROOMS")
public class Chatroom {

  public static Chatroom of(String chatroomName, ChatType type) {
    return Chatroom.builder()
        .chatroomName(chatroomName)
        .type(type)
        .build();
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(length = 30)
  private String chatroomName;

  @OneToMany(mappedBy = "chatroom")
  private List<ChatroomParticipant> participants;

  @CreationTimestamp
  @Column(updatable = false, nullable = false)
  private LocalDateTime createdDate;

  @Enumerated(value = EnumType.STRING)
  private ChatType type;

  public boolean isParticipant(User user){

    if (participants.size() != 2) throw new RingoException(
        "chat participant 2미만",
        ErrorCode.INTERNAL_SERVER_ERROR
    );

    return participants.stream()
        .filter(p -> !p.isWithdrawn())
        .map(ChatroomParticipant::getParticipant)
        .map(User::getId)
        .anyMatch(id -> id.equals(user.getId()));
  }

  public ChatroomParticipant getOpponent(User user){

    validateParticipantSize();

    ChatroomParticipant cp1 = participants.get(0);
    ChatroomParticipant cp2 = participants.get(1);

    return Objects.equals(cp1.getParticipant().getId(), user.getId()) ? cp2 : cp1;
  }

  public ChatroomParticipant getSelf(User user){

    validateParticipantSize();

    ChatroomParticipant cp1 = participants.get(0);
    ChatroomParticipant cp2 = participants.get(1);

    return Objects.equals(cp1.getParticipant().getId(), user.getId()) ? cp1 : cp2;
  }

  public boolean userIsActive(User user){
    return getSelf(user).isActive();
  }

  private void validateParticipantSize(){
    if (participants.size() != 2) throw new RingoException(
        "chat participant 2미만",
        ErrorCode.INTERNAL_SERVER_ERROR
    );
  }

  public List<User> getNonWithdrawnParticipant(){
    return participants.stream()
        .filter(cp -> !cp.isWithdrawn())
        .map(ChatroomParticipant::getParticipant)
        .toList();
  }

}
