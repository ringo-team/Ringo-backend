package com.lingo.lingoproject.user.application;

import com.lingo.lingoproject.shared.domain.model.Drinking;
import com.lingo.lingoproject.shared.domain.model.FcmToken;
import com.lingo.lingoproject.shared.domain.model.FriendInvitationLog;
import com.lingo.lingoproject.shared.domain.model.Hashtag;
import com.lingo.lingoproject.shared.domain.model.Religion;
import com.lingo.lingoproject.shared.domain.model.Smoking;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.domain.model.UserPoint;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import com.lingo.lingoproject.shared.infrastructure.persistence.FcmTokenRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.FriendInvitationLogRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.HashtagRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.UserPointRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.UserRepository;
import com.lingo.lingoproject.shared.utils.GenericUtils;
import com.lingo.lingoproject.user.presentation.dto.SignupInfoDto;
import com.lingo.lingoproject.user.presentation.dto.SignupUserInfoDto;
import jakarta.transaction.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 회원가입 관련 Use Case
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SignupUseCase {

  private static final int FRIEND_INVITATION_REWARD = 10;
  private static final int MAX_INVITATION_INPUT_ATTEMPTS = 5;
  private static final List<String> MBTI_TYPE_LIST = List.of(
      "ESTJ", "ESTP", "ESFJ", "ESFP",
      "ENTJ", "ENTP", "ENFJ", "ENFP",
      "ISTJ", "ISTP", "ISFJ", "ISFP",
      "INTJ", "INTP", "INFJ", "INFP"
  );

  private final PasswordEncoder passwordEncoder;
  private final UserRepository userRepository;
  private final HashtagRepository hashtagRepository;
  private final UserPointRepository userPointRepository;
  private final FcmTokenRepository fcmTokenRepository;
  private final FriendInvitationLogRepository friendInvitationLogRepository;
  private final ApplicationEventPublisher eventPublisher;

  public User signup(SignupInfoDto dto) {
    validateLoginDto(dto);
    return userRepository.save(User.forSignup(dto.loginId(), passwordEncoder.encode(dto.password()), dto.isMarketingReceptionConsent()));
  }

  public boolean verifyDuplicatedLoginId(String loginId) {
    return userRepository.existsByLoginId(loginId);
  }

  public boolean verifyDuplicatedNickname(String nickname) {
    return userRepository.existsByNickname(nickname);
  }

  @Transactional
  public void saveUserInfo(SignupUserInfoDto dto) {
    User user = userRepository.findById(dto.id())
        .orElseThrow(() -> new RingoException("해당 회원을 찾을 수 없습니다.", ErrorCode.USER_NOT_FOUND));

    validateSignupUserAndDto(user, dto);
    user.setUserInfo(dto);
    List<Hashtag> hashtags = buildHashtags(dto, user);

    if (user.getFriendInvitationCode() != null && !user.getFriendInvitationCode().isBlank()) {
      try {
        userRepository.save(user);
        hashtagRepository.deleteAllByUser(user);
        hashtagRepository.saveAll(hashtags);
        return;
      } catch (DataIntegrityViolationException e) {
        log.error("step=회원가입_데이터_무결성_위반, userId={}", user.getId(), e);
        throw new RingoException(e.getMessage(), ErrorCode.INTERNAL_SERVER_ERROR);
      } catch (Exception e) {
        log.error("step=회원가입_오류_발생, userId={}", user.getId(), e);
        throw new RingoException(e.getMessage(), ErrorCode.INTERNAL_SERVER_ERROR);
      }
    }

    user.setFriendInvitationCode(generateFriendInvitationCode());
    try {
      userRepository.save(user);
      hashtagRepository.saveAll(hashtags);
      userPointRepository.save(UserPoint.of(user));
      fcmTokenRepository.save(FcmToken.of(user));
    } catch (DataIntegrityViolationException e) {
      log.error("step=회원가입_데이터_무결성_위반, userId={}", user.getId(), e);
      throw new RingoException(e.getMessage(), ErrorCode.INTERNAL_SERVER_ERROR);
    } catch (Exception e) {
      log.error("step=회원가입_오류_발생, userId={}", user.getId(), e);
      throw new RingoException(e.getMessage(), ErrorCode.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * 친구초대 코드를 검증하고 호스트와 친구 양측에 보상을 제공한다.
   */
  @Transactional
  public void checkFriendInvitationCodeAndProvideReward(User user, String code) {
    boolean alreadyRewarded =
        friendInvitationLogRepository.getNumberOfParticipation(user.getId(), true) > 0;
    boolean exceededAttempts =
        friendInvitationLogRepository.getNumberOfParticipation(user.getId(), false)
            > MAX_INVITATION_INPUT_ATTEMPTS;

    if (alreadyRewarded || exceededAttempts) {
      throw new RingoException(
          "이미 완료한 이벤트거나 입력횟수를 초과하였습니다.", ErrorCode.PROFILE_DUPLICATED);
    }

    Optional<User> host = userRepository.findByFriendInvitationCode(code);
    if (host.isEmpty()) {
      friendInvitationLogRepository.save(FriendInvitationLog.of(null, user.getId(), false));
      throw new RingoException("친구초대코드를 잘못 입력하였습니다.", ErrorCode.BAD_REQUEST);
    }

    friendInvitationLogRepository.save(FriendInvitationLog.of(host.get().getId(), user.getId(), true));
    userPointRepository.updateUserPoint(FRIEND_INVITATION_REWARD, user);
    userPointRepository.updateUserPoint(FRIEND_INVITATION_REWARD, host.get());
  }

  public String generateFriendInvitationCode() {
    return Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes()).substring(0, 8);
  }

  private void validateLoginDto(SignupInfoDto dto) {
    if (!dto.loginId().matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]+$")) {
      log.warn("회원가입 로그인 요청값: {}", dto.loginId());
      throw new RingoException("적절하지 않은 입력값입니다.", ErrorCode.BAD_PARAMETER);
    }
    if (!dto.password().matches("^(?=.*[A-Za-z])(?=.*\\d).{8,}$")) {
      throw new RingoException("적절하지 않은 입력값입니다.", ErrorCode.BAD_PARAMETER);
    }
    if (userRepository.existsByLoginId(dto.loginId())) {
      throw new RingoException("중복된 로그인 아이디 입니다.", ErrorCode.PROFILE_DUPLICATED);
    }
  }

  private void validateSignupUserAndDto(User user, SignupUserInfoDto dto) {
    if (LocalDate.parse(dto.birthday()).getYear() + 19 > LocalDate.now().getYear()) {
      throw new RingoException("미성년자는 회원가입이 불가합니다.", ErrorCode.NOT_ADULT);
    }
    GenericUtils.validateAndReturnEnumValue(Smoking.values(), dto.isSmoking());
    GenericUtils.validateAndReturnEnumValue(Drinking.values(), dto.isDrinking());
    GenericUtils.validateAndReturnEnumValue(Religion.values(), dto.religion());
    if (!MBTI_TYPE_LIST.contains(dto.mbti().toUpperCase())) {
      throw new RingoException("mbti 카테고리에 포함되지 않습니다.", ErrorCode.BAD_PARAMETER);
    }
    if (!(dto.gender().equalsIgnoreCase("MALE") || dto.gender().equalsIgnoreCase("FEMALE"))) {
      throw new RingoException("성별 카테고리에 포함되지 않습니다.", ErrorCode.BAD_PARAMETER);
    }
  }

  private List<Hashtag> buildHashtags(SignupUserInfoDto dto, User user) {
    List<Hashtag> hashtags = new ArrayList<>();
    for (String tag : dto.hashtag()) {
      hashtags.add(Hashtag.of(user, tag));
    }
    return hashtags;
  }
}