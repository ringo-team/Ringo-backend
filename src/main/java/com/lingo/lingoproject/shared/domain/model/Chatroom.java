package com.lingo.lingoproject.shared.domain.model;

import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Slf4j
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

  @OneToMany(mappedBy = "chatroom", fetch = FetchType.EAGER)
  private List<ChatroomParticipant> participants;

  @CreationTimestamp
  @Column(updatable = false, nullable = false)
  private LocalDateTime createdDate;

  @Enumerated(value = EnumType.STRING)
  private ChatType type;

  public boolean 채팅방에_속하는지(User user){

    log.info("participant size = {}", participants.size());

    if (participants.size() != 2) throw new RingoException(
        "chat participant 2미만",
        ErrorCode.INTERNAL_SERVER_ERROR
    );

    boolean result = participants.stream()
        .filter(p -> !p.is회원탈퇴한_유저인지())
        .map(ChatroomParticipant::getParticipant)
        .map(User::getId)
        .anyMatch(id -> id.equals(user.getId()));

    log.info("isparticipant result={}", result);
    return result;
  }

  public ChatroomParticipant 채팅_상대방_유저_조회(User user){

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

  public boolean 유저가_채팅방을_나갔는지(User user){
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
        .filter(cp -> !cp.is회원탈퇴한_유저인지())
        .map(ChatroomParticipant::getParticipant)
        .toList();
  }

}
