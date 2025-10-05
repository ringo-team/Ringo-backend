package com.lingo.lingoproject.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "DORMANT_ACCOUNTS")
public class DormantAccount {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /*
  휴면 계정은 친구 추천에 들어가면 안됨;
   */
  @ManyToOne
  @JoinColumn(name = "user_id")
  private User user;
}
