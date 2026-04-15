package com.lingo.lingoproject.db.domain;

import com.lingo.lingoproject.db.domain.enums.Gender;
import com.lingo.lingoproject.db.domain.enums.MemberShipType;
import com.lingo.lingoproject.common.utils.Timestamp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberShipLog extends Timestamp {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(updatable = false)
  private Long userId;
  @Column(length = 10, updatable = false)
  private String nickname;
  @Column(length = 10, updatable = false)
  private String username;

  @Enumerated(value = EnumType.STRING)
  @Column(updatable = false)
  private Gender gender;

  private int age;


  @Enumerated(value = EnumType.STRING)
  @Column(updatable = false)
  private MemberShipType type;

  private LocalDateTime endTime;
}
