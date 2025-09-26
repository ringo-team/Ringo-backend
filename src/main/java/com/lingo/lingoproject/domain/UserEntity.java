package com.lingo.lingoproject.domain;

import com.lingo.lingoproject.domain.enums.Gender;
import com.lingo.lingoproject.domain.enums.HashTag;
import com.lingo.lingoproject.domain.enums.Religion;
import com.lingo.lingoproject.domain.enums.Role;
import com.lingo.lingoproject.domain.enums.SignUpStatus;
import com.lingo.lingoproject.utils.Timestamp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.Collection;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class UserEntity extends Timestamp implements UserDetails {

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of();
  }

  @Override
  public String getUsername() {
    return this.email;
  }

  @Override
  public String getPassword() {
    return this.password;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String nickname;

  @Column(unique = true)
  private String email;
  private String password;

  @Column(unique = true)
  private String phoneNumber;

  @Column(unique = true)
  private String deviceToken;

  @Enumerated(EnumType.STRING)
  private Gender gender;

  private String height;
  private boolean isSmoking;
  private boolean isDrinking;

  @Enumerated(EnumType.STRING)
  private Role role;

  @Enumerated(EnumType.STRING)
  private Religion religion;

  private String job;

  /*
   * 회원가입 진행상황
   *   시작전
   *   진행중
   *   완료
   */
  private SignUpStatus status;

  private boolean isActive;
  private String etc;
}
