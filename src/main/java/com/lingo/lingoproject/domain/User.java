package com.lingo.lingoproject.domain;

import com.lingo.lingoproject.auth.dto.UserSelfAuthInfo;
import com.lingo.lingoproject.domain.enums.Drinking;
import com.lingo.lingoproject.domain.enums.Gender;
import com.lingo.lingoproject.domain.enums.Nation;
import com.lingo.lingoproject.domain.enums.Religion;
import com.lingo.lingoproject.domain.enums.Role;
import com.lingo.lingoproject.domain.enums.SignupStatus;
import com.lingo.lingoproject.domain.enums.Smoking;
import com.lingo.lingoproject.exception.ErrorCode;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.security.controller.dto.SignupUserInfoDto;
import com.lingo.lingoproject.utils.Timestamp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@DynamicInsert
@Table(name = "USERS")
public class User extends Timestamp implements UserDetails {

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + this.role));
  }

  @Override
  public String getUsername() {
    return this.loginId;
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
  private String loginId;
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

  private String activityLocFirstPlace;
  private String activityLocSecondPlace;


  @Enumerated(EnumType.STRING)
  private Smoking isSmoking;

  @Enumerated(EnumType.STRING)
  private Drinking isDrinking;


  @Enumerated(EnumType.STRING)
  private Role role;

  @Enumerated(EnumType.STRING)
  private Religion religion;

  @Column(length = 100)
  private String biography;

  private String job;

  /**
   * 회원가입 진행상황
   *   시작전
   *   진행중
   *   완료
   */
  @Enumerated(EnumType.STRING)
  private SignupStatus status;

  private String friendInvitationCode;

  // 마케팅 수신 동의 여부
  @ColumnDefault(value = "false")
  private Boolean isMarketingReceptionConsent;

  public void setUserInfo(SignupUserInfoDto dto){
    this.nickname = dto.nickname();
    this.residenceFirstPlace = dto.address().city();
    this.residenceSecondPlace = dto.address().district();
    this.activityLocFirstPlace = dto.activeAddress().city();
    this.activityLocSecondPlace = dto.activeAddress().district();
    this.job = dto.job();
    this.height = dto.height();
    this.isSmoking = Smoking.valueOf(dto.isSmoking());
    this.isDrinking = Drinking.valueOf(dto.isDrinking());
    this.religion = Religion.valueOf(dto.religion());
    this.biography = dto.biography();
    this.status = SignupStatus.IN_PROGRESS;
  }

  public void setUserSelfAuthInfo(UserSelfAuthInfo dto){

    DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
    try {
      this.mobileCarrier = dto.getMobileCarrier();
      this.phoneNumber = dto.getPhoneNumber();
      this.name = dto.getName();
      this.nationalInfo = Nation.valueOf(dto.getNationalInfo());
      this.birthday = dateFormat.parse(dto.getBirthday());
      this.gender = Gender.valueOf(dto.getGender());
    } catch (Exception e) {
      throw new RingoException(e.getMessage(), ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
