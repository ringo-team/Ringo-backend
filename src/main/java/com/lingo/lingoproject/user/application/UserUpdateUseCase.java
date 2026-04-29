package com.lingo.lingoproject.user.application;

import com.lingo.lingoproject.shared.domain.model.Drinking;
import com.lingo.lingoproject.shared.domain.model.FaceVerify;
import com.lingo.lingoproject.shared.domain.model.Hashtag;
import com.lingo.lingoproject.shared.domain.model.Profile;
import com.lingo.lingoproject.shared.domain.model.Religion;
import com.lingo.lingoproject.shared.domain.model.Smoking;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import com.lingo.lingoproject.shared.infrastructure.persistence.HashtagRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.ProfileRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.UserRepository;
import com.lingo.lingoproject.shared.utils.GenericUtils;
import com.lingo.lingoproject.user.presentation.dto.Address;
import com.lingo.lingoproject.user.presentation.dto.UpdateUserInfoRequestDto;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 유저 정보 수정 / 프로필 인증 Use Case
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserUpdateUseCase {

  private static final List<String> MBTI_TYPE_LIST = List.of(
      "ESTJ", "ESTP", "ESFJ", "ESFP",
      "ENTJ", "ENTP", "ENFJ", "ENFP",
      "ISTJ", "ISTP", "ISFJ", "ISFP",
      "INTJ", "INTP", "INFJ", "INFP"
  );

  private final UserRepository userRepository;
  private final HashtagRepository hashtagRepository;
  private final ProfileRepository profileRepository;
  private final PasswordEncoder passwordEncoder;
  private final RedisTemplate<String, Object> redisTemplate;

  @Transactional
  public void updateUserInfo(User user, UpdateUserInfoRequestDto dto) {
    GenericUtils.validateAndSetEnum(dto.isDrinking(), Drinking.values(), user::setIsDrinking);
    GenericUtils.validateAndSetEnum(dto.isSmoking(), Smoking.values(), user::setIsSmoking);
    GenericUtils.validateAndSetEnum(dto.religion(), Religion.values(), user::setReligion);
    GenericUtils.validateAndSetStringValue(dto.job(), user::setJob);
    GenericUtils.validateAndSetStringValue(dto.height(), user::setHeight);
    GenericUtils.validateAndSetStringValue(dto.degree(), user::setDegree);
    GenericUtils.validateAndSetStringValue(dto.biography(), user::setBiography);
    GenericUtils.validateAndSetStringValue(dto.nickname(), user::setNickname);

    log.info("[BIO] :{}, {}", user.getBiography(), dto.hashtag());

    Address activeAddress = dto.activeAddress();
    if (isValidAddress(activeAddress)) {
      user.setActivityLocProvince(activeAddress.province());
      user.setActivityLocCity(activeAddress.city());
    }

    if (dto.hashtag() != null && !dto.hashtag().isEmpty()) {
      hashtagRepository.deleteAllByUser(user);
      List<Hashtag> hashtagEntities = dto.hashtag().stream()
          .map(h -> Hashtag.of(user, h))
          .toList();
      hashtagRepository.saveAll(hashtagEntities);
    }


    if (dto.mbti() != null) setUserMbti(user, dto.mbti());
    userRepository.save(user);
  }

  public void resetPassword(String newPassword, User user) {
    boolean isAuthenticated = Boolean.TRUE.equals(redisTemplate.hasKey("self-auth::" + user.getId()));
    if (!isAuthenticated) {
      throw new RingoException("비밀번호를 재설정할 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
    }
    user.setPassword(passwordEncoder.encode(newPassword));
    userRepository.save(user);
  }

  public void updateUserProfileVerification(User user) {
    Profile profile = user.getProfile();
    profile.setFaceVerify(FaceVerify.PASS);
    profileRepository.save(profile);
  }

  /** 주소 유효성 검증 - 버그 수정: isBlank() 반전 오류 수정 */
  private boolean isValidAddress(Address address) {
    return address != null
        && address.province() != null && !address.province().isBlank()
        && address.city() != null && !address.city().isBlank();
  }

  private void setUserMbti(User user, String mbti) {
    if (!MBTI_TYPE_LIST.contains(mbti.toUpperCase())) {
      throw new RingoException("mbti 카테고리에 포함되지 않습니다.", ErrorCode.BAD_PARAMETER, HttpStatus.BAD_REQUEST);
    }
    user.setMbti(mbti.toUpperCase());
  }
}
