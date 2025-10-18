package com.lingo.lingoproject.domain;


import com.lingo.lingoproject.domain.enums.Gender;
import com.lingo.lingoproject.utils.Timestamp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "USER_ACCESS_LOGS")
@EntityListeners(AuditingEntityListener.class)
public class UserAccessLog{
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(updatable = false)
  private Long userId;

  @Column(updatable = false)
  private String username;

  @Enumerated(EnumType.STRING)
  @Column(updatable = false)
  private Gender gender;

  @Column(updatable = false)
  private Integer age;

  @CreationTimestamp
  @Column(updatable = false)
  private LocalDateTime createAt;
}
