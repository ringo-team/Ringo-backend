package com.lingo.lingoproject.domain;

import com.lingo.lingoproject.utils.Timestamp;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import org.hibernate.annotations.ColumnDefault;

@Entity
public class UserActivityLogs extends Timestamp {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne
  private UserEntity user;

  @ColumnDefault("0")
  private Long connect;
  private Long matchingNum;
  private Long matchReqNum;
  private Long matchTakenNum;
}
