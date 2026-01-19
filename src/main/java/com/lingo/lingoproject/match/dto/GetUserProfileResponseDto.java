package com.lingo.lingoproject.match.dto;

import com.lingo.lingoproject.domain.enums.Gender;
import com.lingo.lingoproject.domain.enums.MatchingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Data 어노테이션은 다음과 같은 어노테이션을 포함하고 있다.
 * - @ToString
 * - @EqualsAndHashCode
 * - @Getter
 * - @Setter
 * - @RequiredArgsConstructor
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetUserProfileResponseDto {

  @Schema(description = "유저 id", example = "5")
  private Long userId;

  @Schema(description = "유저 나이", example = "23")
  private Integer age;

  @Schema(description = "유저 성별", example = "MALE")
  private String gender;

  @Schema(description = "유저 닉네임", example = "불타는 망고")
  private String nickname;

  @Schema(description = "이미지 url")
  private String profileUrl;

  @Schema(description = "연결 적합도", example = "75")
  private Float matchingScore;

  @Schema(description = "매칭 id", example = "199")
  private Long matchingId;

  @Schema(description = "매칭 여부", example = "PENDING", allowableValues = {"PENDING", "ACCEPTED", "REJECTED"})
  private String matchingStatus;

  @Schema(description = "매칭 메세지", example = "안녕하세요")
  private String matchingMessage;

  @Schema(description = "해시태그", example = "[\"운동\", \"건강\"]")
  private List<String> hashtags;

  @Schema(description = "숨김 여부, 1 -> 숨김, 0 -> 노출", example = "1")
  private Integer hide;

  @Schema(description = "얼굴 인증 여부, 1 -> 인증됨, 0 -> 인증 안됨", example = "1")
  private Integer verify;

  @Schema(description = "오늘로부터 마지막으로 접속한 시간 사이의 기간", example = "5")
  private Integer daysFromLastAccess;

  @Schema(description = "mbti", example = "ESFP")
  private String mbti;


  public GetUserProfileResponseDto(
      Long userId,
      Integer age,
      Gender gender,
      String nickname,
      String profileUrl,
      Float matchingScore,
      Long matchingId,
      MatchingStatus matchingStatus,
      String matchingMessage,
      Boolean verify
  ) {
    this.userId = userId;
    this.age = age;
    this.gender = gender.toString();
    this.nickname = nickname;
    this.profileUrl = profileUrl;
    this.matchingScore = matchingScore;
    this.matchingId = matchingId;
    this.matchingStatus = matchingStatus.toString();
    this.matchingMessage = matchingMessage;
    this.verify = verify ? 1 : 0;
  }
}
