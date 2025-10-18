package com.lingo.lingoproject.domain;

import com.lingo.lingoproject.domain.enums.Gender;
import com.lingo.lingoproject.domain.enums.Nation;
import com.lingo.lingoproject.domain.enums.Religion;
import com.lingo.lingoproject.domain.enums.Role;
import com.lingo.lingoproject.domain.enums.SignupStatus;
import com.lingo.lingoproject.utils.Timestamp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "USERS")
public class User extends Timestamp implements UserDetails {

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + this.role));
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

  @Column(unique = true)
  private String nickname;

  private String name;

  @Column(unique = true)
  private String email;
  private String password;

  @Column(unique = true)
  private String phoneNumber;


  /**
   * 이동 통신사
   * 1: SKT
   * 2: KT
   * 3: LGU+
   * 4: SKT 알뜰폰
   * 5: KT 알뜰폰
   * 6: LGU+ 알뜰폰
   */
  private String mobileCarrier;

  @Enumerated(EnumType.STRING)
  private Nation nationalInfo;

  private Date birthday;

  @Column(unique = true)
  private String deviceToken;

  @Enumerated(EnumType.STRING)
  private Gender gender;


  private String height;
  private Integer age;

  private String residenceFirstPlace;
  private String residenceSecondPlace;

  private String activityLocFirstsPlace;
  private String activityLocSecondsPlace;


  private Boolean isSmoking;
  private Boolean isDrinking;


  @Enumerated(EnumType.STRING)
  private Role role;

  @Enumerated(EnumType.STRING)
  private Religion religion;

  private String job;

  /**
   * 회원가입 진행상황
   *   시작전
   *   진행중
   *   완료
   */
  private SignupStatus status;

  private Boolean isActive;
  private String etc;
}
