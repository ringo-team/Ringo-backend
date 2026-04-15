package com.lingo.lingoproject.shared.domain.model;

import com.lingo.lingoproject.user.presentation.dto.auth.UserSelfAuthInfo;
import com.lingo.lingoproject.shared.domain.model.Drinking;
import com.lingo.lingoproject.shared.domain.model.Gender;
import com.lingo.lingoproject.shared.domain.model.Nation;
import com.lingo.lingoproject.shared.domain.model.Religion;
import com.lingo.lingoproject.shared.domain.model.Role;
import com.lingo.lingoproject.shared.domain.model.SignupStatus;
import com.lingo.lingoproject.shared.domain.model.Smoking;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import com.lingo.lingoproject.user.presentation.dto.SignupUserInfoDto;
import com.lingo.lingoproject.shared.utils.Timestamp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

/**
 * 서비스의 핵심 사용자 엔티티 (USERS 테이블).
 *
 * <p>Spring Security의 {@link UserDetails}를 구현하여 인증/인가 시스템과 직접 연동됩니다.
 * SecurityContext에 저장되는 Principal 객체가 바로 이 User 엔티티입니다.
 * 컨트롤러에서 {@code @AuthenticationPrincipal User user} 파라미터로 현재 로그인 유저를 바로 주입받을 수 있습니다.</p>
 *
 * <p>회원가입 흐름:
 * <ol>
 *   <li>일반 가입: {@code forSignup()} → status=IN_PROGRESS</li>
 *   <li>OAuth 가입(구글/카카오): {@code forOAuthSignup()} → status=BEFORE</li>
 *   <li>추가 정보 입력: {@code setUserInfo()} → 필드 채움</li>
 *   <li>본인인증(NICE): {@code setUserSelfAuthInfo()} → 전화번호/실명 인증</li>
 *   <li>프로필 사진 등록 후 status=COMPLETED → 전체 서비스 이용 가능</li>
 * </ol>
 * </p>
 *
 * <p>{@code @DynamicInsert}: INSERT 시 null 필드는 쿼리에서 제외하여 DB 기본값(ColumnDefault)이 적용되도록 합니다.</p>
 *
 * <p>역할(Role):
 * <ul>
 *   <li>USER: 일반 사용자</li>
 *   <li>ADMIN: 관리자 — /admin/** 엔드포인트 접근 가능</li>
 *   <li>PHOTOGRAPHER: 스냅 작가 — /photographers/** 엔드포인트 접근 가능</li>
 * </ul>
 * </p>
 */
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@DynamicInsert
@Table(name = "USERS")
public class User extends Timestamp implements UserDetails {

  /**
   * Spring Security가 권한 목록을 조회할 때 호출합니다.
   * role 값에 "ROLE_" 접두사를 붙여 반환합니다 (Spring Security 규약).
   * 예: Role.ADMIN → "ROLE_ADMIN"
   */
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + this.role));
  }

  /**
   * Spring Security가 사용하는 사용자 식별자 (JWT subject 필드와 동일).
   * loginId를 username으로 사용합니다.
   */
  @Override
  public String getUsername() {
    return this.loginId;
  }

  /** BCrypt로 인코딩된 비밀번호를 반환합니다. */
  @Override
  public String getPassword() {
    return this.password;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** 서비스 내 표시 이름. 최대 15자, 유일해야 합니다. */
  @Column(length = 15, unique = true)
  private String nickname;

  /** 실명 (본인인증 후 저장). */
  @Column(length = 10)
  private String name;

  /** 로그인 ID. JWT의 subject 클레임으로도 사용됩니다. */
  @Column(length = 20, unique = true)
  private String loginId;

  /** BCrypt 인코딩된 비밀번호. OAuth 유저는 null입니다. */
  private String password;

  /** 본인인증(NICE) 후 저장되는 휴대폰 번호. */
  @Column(length = 13, unique = true)
  private String phoneNumber;

  /** 프로필 이미지 정보 (1:1 관계). */
  @OneToOne
  @JoinColumn(name = "profile_id")
  private Profile profile;

  /**
   * 이동 통신사 코드 (본인인증 NICE API에서 반환).
   * 1: SKT, 2: KT, 3: LGU+
   * 4: SKT 알뜰폰, 5: KT 알뜰폰, 6: LGU+ 알뜰폰
   */
  @Column(length = 10)
  private String mobileCarrier;

  /** 내국인/외국인 구분 (DOMESTIC / FOREIGN). 본인인증 후 저장. */
  @Enumerated(EnumType.STRING)
  private Nation nationalInfo;

  /** 생년월일. 본인인증 후 저장 (yyyyMMdd 문자열을 파싱하여 저장). */
  private LocalDate birthday;

  /** 푸시 알림용 디바이스 고유 토큰. */
  @Column(unique = true)
  private String deviceToken;

  /** 성별 (MALE / FEMALE). */
  @Enumerated(EnumType.STRING)
  private Gender gender;

  /** 키 (cm 단위 문자열). */
  @Column(length = 5)
  private String height;

  /** 최종 학력 (예: "대졸"). */
  @Column(length = 10)
  private String degree;

  /** 거주지 도/광역시 (예: "서울특별시"). */
  @Column(length = 10)
  private String residenceProvince;

  /** 거주지 시/구 (예: "강남구"). */
  @Column(length = 10)
  private String residenceCity;

  /** 활동 지역 도/광역시. */
  @Column(length = 10)
  private String activityLocProvince;

  /** 활동 지역 시/구. */
  @Column(length = 10)
  private String activityLocCity;

  /** 흡연 여부 (YES / NO / SOMETIMES). */
  @Enumerated(EnumType.STRING)
  private Smoking isSmoking;

  /** 음주 여부 (YES / NO / SOMETIMES). */
  @Enumerated(EnumType.STRING)
  private Drinking isDrinking;

  /**
   * 사용자 역할.
   * USER: 일반 사용자 / ADMIN: 관리자 / PHOTOGRAPHER: 스냅 작가
   */
  @Enumerated(EnumType.STRING)
  private Role role;

  /** 종교 (NONE / CHRISTIAN / CATHOLIC / BUDDHISM / OTHER). */
  @Enumerated(EnumType.STRING)
  private Religion religion;

  /** 자기소개 문구. 최대 100자. */
  @Column(length = 100)
  private String biography;

  /** 직업 (예: "개발자"). */
  @Column(length = 10)
  private String job;

  /** 직장 이름. */
  @Column(length = 15)
  private String workPlace;

  /** 학교 이름. */
  @Column(length = 15)
  private String schoolName;

  /**
   * 회원가입 진행 상태.
   * BEFORE: OAuth 최초 진입 (추가 정보 미입력)
   * IN_PROGRESS: 정보 입력 중 (아직 일부 기능 제한)
   * COMPLETED: 가입 완료 (모든 기능 이용 가능)
   */
  @Enumerated(EnumType.STRING)
  private SignupStatus status;

  /** 친구 초대 코드. 타 유저를 초대할 때 사용합니다. */
  @Column(length = 10)
  private String friendInvitationCode;

  /** MBTI 유형 (예: "ENFP"). */
  @Column(length = 10)
  private String mbti;

  /**
   * 일반 회원가입용 정적 팩토리 메서드.
   * 로그인 ID와 BCrypt 인코딩된 비밀번호만 가지고 유저를 생성합니다.
   * status는 IN_PROGRESS로 설정됩니다.
   *
   * @param loginId        사용자가 입력한 로그인 ID
   * @param encodedPassword BCrypt 인코딩된 비밀번호
   */
  public static User forSignup(String loginId, String encodedPassword) {
    return User.builder()
        .loginId(loginId)
        .password(encodedPassword)
        .role(Role.USER)
        .status(SignupStatus.IN_PROGRESS)
        .build();
  }

  /**
   * OAuth(구글/카카오) 신규 유저용 정적 팩토리 메서드.
   * 비밀번호 없이 loginId만으로 유저를 생성합니다.
   * status는 BEFORE로 설정되어 추가 정보 입력을 유도합니다.
   *
   * @param loginId OAuth 제공자에서 받은 고유 ID (예: 구글 이메일)
   */
  public static User forOAuthSignup(String loginId) {
    return User.builder()
        .loginId(loginId)
        .role(Role.USER)
        .status(SignupStatus.BEFORE)
        .build();
  }

  /**
   * 회원가입 추가 정보를 User 엔티티에 반영합니다.
   * 닉네임, 성별, 생년월일, 거주지, 활동지역, 직업, 학교, 키, 생활습관 등을 설정합니다.
   *
   * @param dto 클라이언트에서 전달된 회원 정보 DTO
   */
  public void setUserInfo(SignupUserInfoDto dto){
    this.nickname = dto.nickname();
    this.gender = Gender.valueOf(dto.gender().toUpperCase());
    this.birthday = (LocalDate) DateTimeFormatter.ofPattern("yyyy-MM-dd").parse(dto.birthday());
    this.residenceProvince = dto.address().province();
    this.residenceCity = dto.address().city();
    this.activityLocProvince = dto.activeAddress().province();
    this.activityLocCity = dto.activeAddress().city();
    this.job = dto.job();
    this.workPlace = dto.workPlace();
    this.schoolName = dto.schoolName();
    this.height = dto.height();
    this.isSmoking = Smoking.valueOf(dto.isSmoking());
    this.isDrinking = Drinking.valueOf(dto.isDrinking());
    this.religion = Religion.valueOf(dto.religion());
    this.biography = dto.biography();
    this.status = SignupStatus.IN_PROGRESS;
    this.mbti = dto.mbti();
  }

  /**
   * NICE 본인인증 완료 후 인증 정보를 User 엔티티에 반영합니다.
   * 통신사, 전화번호, 실명, 내/외국인 구분, 생년월일, 성별을 저장합니다.
   * 성별 코드 ("1"→MALE, "2"→FEMALE)와 국적 코드 ("0"→DOMESTIC, "1"→FOREIGN)는
   * {@link com.lingo.lingoproject.user.application.SelfAuthUseCase#deserializeAndSaveUserInfo}에서 이미 변환된 상태로 전달됩니다.
   *
   * @param dto NICE API 응답에서 파싱된 본인인증 정보
   */
  public void setUserSelfAuthInfo(UserSelfAuthInfo dto){

    try {
      this.mobileCarrier = dto.getMobileCarrier();
      this.phoneNumber = dto.getPhoneNumber();
      this.name = dto.getName();
      this.nationalInfo = Nation.valueOf(dto.getNationalInfo());
      this.birthday = (LocalDate) DateTimeFormatter.ofPattern("yyyyMMdd").parse(dto.getBirthday());
      this.gender = Gender.valueOf(dto.getGender());
    } catch (Exception e) {
      throw new RingoException(e.getMessage(), ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
