package com.lingo.lingoproject.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Table(name = "FRIEND_INVITATION_LOGS")
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FriendInvitationLog {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long hostId;
  private Long friendId;
  private Boolean isRealCode;
}
