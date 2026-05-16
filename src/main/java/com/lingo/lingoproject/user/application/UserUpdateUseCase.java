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
import com.lingo.lingoproject.shared.utils.GenericUtils;
import com.lingo.lingoproject.user.presentation.dto.Address;
import com.lingo.lingoproject.user.presentation.dto.UpdateUserInfoRequestDto;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
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

  private final UserQueryUseCase userQueryUseCase;
  private final HashtagRepository hashtagRepository;
  private final ProfileRepository profileRepository;
  private final PasswordEncoder passwordEncoder;
  private final RedisTemplate<String, Object> redisTemplate;

  @Transactional
  public void updateUserInfo(User user, UpdateUserInfoRequestDto dto) {
    GenericUtils.enum_검증후_set(dto.isDrinking(), Drinking.values(), user::setIsDrinking);
    GenericUtils.enum_검증후_set(dto.isSmoking(), Smoking.values(), user::setIsSmoking);
    GenericUtils.enum_검증후_set(dto.religion(), Religion.values(), user::setReligion);
    GenericUtils.문자열_널_검증_후_set(dto.job(), user::setJob);
    GenericUtils.문자열_널_검증_후_set(dto.height(), user::setHeight);
    GenericUtils.문자열_널_검증_후_set(dto.degree(), user::setDegree);
    GenericUtils.문자열_널_검증_후_set(dto.biography(), user::setBiography);
    GenericUtils.문자열_널_검증_후_set(dto.nickname(), user::setNickname);

    log.info("[BIO] :{}, {}", user.getBiography(), dto.hashtag());

    Address activeAddress = dto.activeAddress();
    if (isValidAddress(activeAddress)) {
      com.lingo.lingoproject.shared.domain.model.Address activityAddress
          = new com.lingo.lingoproject.shared.domain.model.Address(
              activeAddress.province(),
              activeAddress.city()
      );
      user.setActivityAddress(activityAddress);
    }

    if (dto.hashtag() != null && !dto.hashtag().isEmpty()) {
      hashtagRepository.deleteAllByUser(user);
      List<Hashtag> hashtagEntities = dto.hashtag().stream()
          .map(h -> Hashtag.of(user, h))
          .toList();
      hashtagRepository.saveAll(hashtagEntities);
    }

    if (dto.school() != null && !dto.school().isBlank()) user.setSchoolName(dto.school());
    if (dto.mbti() != null) setUserMbti(user, dto.mbti());

    userQueryUseCase.save(user);
  }

  public void resetPassword(String newPassword, User user) {
    boolean isAuthenticated = redisTemplate.hasKey("self-auth::" + user.getId());
    if (!isAuthenticated) {
      throw new RingoException("비밀번호를 재설정할 권한이 없습니다.", ErrorCode.NO_AUTH);
    }
    user.setPassword(passwordEncoder.encode(newPassword));
    userQueryUseCase.save(user);
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
      throw new RingoException("mbti 카테고리에 포함되지 않습니다.", ErrorCode.BAD_PARAMETER);
    }
    user.setMbti(mbti.toUpperCase());
  }
}
