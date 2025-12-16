package com.lingo.lingoproject.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(
    name = "FRIEND_INVITATION_LOGS",
    indexes = {
        @Index(
            name = "idx_friend_invitation_logs_friend_id_is_real_code",
            columnList = "friendId, isRealCode"
        )
    }
)
public class FriendInvitationLog {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(updatable = false)
  private Long hostId;

  @Column(updatable = false)
  private Long friendId;

  @Column(updatable = false)
  private Boolean isRealCode;
}
