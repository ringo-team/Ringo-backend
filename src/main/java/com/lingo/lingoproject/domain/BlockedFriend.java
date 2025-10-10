package com.lingo.lingoproject.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import jakarta.persistence.Table;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "BLOCKED_FRIENDS")
public class BlockedFriend {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "user_id")
  private User user;

  /*
  회원 A에게 친구추천시
  상대방 B의 blocking list에
  A의  전화번호가 들어있으면 안됨
   */
  private String phoneNumber;
}
