package com.lingo.lingoproject.user.presentation.dto;

import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import lombok.Builder;

@Builder
public record GetUserInfoResponseDto(

    @Schema(description = "유저 id", example = "4")
    Long userId,

    @Schema(description = "활동지")
    Address activeAddress,

    @Schema(description = "닉네임", example = "불타는 망고")
    String nickname,

    @Schema(description = "프로필 url", example = "url")
    String profile,

    @Schema(description = "생년월일", example = "2003-02-06")
    String birthday,

    @Schema(description = "성별", example = "MALE", allowableValues = {"MALE", "FEMALE"})
    String gender,

    @Schema(description = "mbti", example = "ENFP")
    String mbti,

    @Schema(description = "키", example = "177")
    String height,

    @Schema(description = "음주여부", example = "OFTEN", allowableValues = {"ALWAYS", "OFTEN", "RARELY", "ON_NEED", "NEVER"})
    String isDrinking,

    @Schema(description = "흡연여부", example = "NEVER", allowableValues = {"SMOKING", "ELECTRONIC", "NO_SMOKING", "NEVER"})
    String isSmoking,

    @Schema(description = "직업", example = "개발자")
    String job,

    @Schema(description = "직장명", example = "하이닉스")
    String workPlace,

    @Schema(description = "종교", example = "CHRISTIANITY",
        allowableValues = {"CHRISTIANITY", "BUDDHISM", "CATHOLIC", "ATHEIST", "ETC"})
    String religion,

    @Schema(description = "소개문구", example = "안녕하세요")
    String biography,

    @Schema(description = "해시태그", example = "[\"운동\", \"건강\"]")
    List<String> hashtags,

    @Schema(description = "학력", example = "대학교 졸업")
    String degree,

    @Schema(description = "학교", example = "서울대")
    String school,

    @Schema(description = "얼굴 인증 여부", example = "true")
    boolean isFaceVerify,

    @Schema(description = "휴면계정 여부", example = "true")
    boolean isDormant,

    Map<String, Boolean> notificationSettingMap,

    @Schema(description = "프로필 완성율", example = "75")
    int profileCompleteRate,

    @Schema(description = "결과코드", example = "0000")
    String result
) {

  public static GetUserInfoResponseDto from(
      User user,
      List<String> hashtags,
      boolean isDormant,
      boolean isFaceVerify,
      Map<String, Boolean> notificationSettingMap,
      int profileCompleteRate
  ) {

    Address address = new Address(
        user.getActivityAddress() != null ? user.getActivityAddress().getProvince() : null,
        user.getActivityAddress() != null ? user.getActivityAddress().getCity() : null
    );

    return GetUserInfoResponseDto.builder()
        .userId(user.getId())
        .activeAddress(address)
        .nickname(user.getNickname())
        .profile(user.getProfile() != null ? user.getProfile().getImageUrl() : null)
        .birthday(user.getBirthday() != null ? user.getBirthday().toString() : null)
        .gender(user.getGender() != null ? user.getGender().toString() : null)
        .mbti(user.getMbti())
        .height(user.getHeight())
        .isDrinking(user.getIsDrinking() != null ? user.getIsDrinking().toString() : null)
        .isSmoking(user.getIsSmoking() != null ? user.getIsSmoking().toString() : null)
        .job(user.getJob())
        .degree(user.getDegree())
        .school(user.getSchoolName())
        .workPlace(user.getWorkPlace())
        .religion(user.getReligion() != null ? user.getReligion().toString() : null)
        .biography(user.getBiography())
        .hashtags(hashtags)
        .isDormant(isDormant)
        .isFaceVerify(isFaceVerify)
        .notificationSettingMap(notificationSettingMap)
        .profileCompleteRate(profileCompleteRate)
        .result(ErrorCode.SUCCESS.getCode())
        .build();
  }

  public static GetUserInfoResponseDto summary(User user) {
    return GetUserInfoResponseDto.builder()
        .userId(user.getId())
        .nickname(user.getNickname())
        .gender(user.getGender() != null ? user.getGender().toString() : null)
        .height(user.getHeight())
        .isDrinking(user.getIsDrinking() != null ? user.getIsDrinking().toString() : null)
        .isSmoking(user.getIsSmoking() != null ? user.getIsSmoking().toString() : null)
        .religion(user.getReligion() != null ? user.getReligion().toString() : null)
        .job(user.getJob())
        .biography(user.getBiography())
        .build();
  }
}
