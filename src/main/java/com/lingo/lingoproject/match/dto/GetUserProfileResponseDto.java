package com.lingo.lingoproject.match.dto;

import com.lingo.lingoproject.domain.enums.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

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
public class GetUserProfileResponseDto {

  @Schema(description = "유저 id", example = "5")
  private final Long userId;

  @Schema(description = "유저 나이", example = "23")
  private final int age;

  @Schema(description = "유저 성별", example = "MALE")
  private final Gender gender;

  @Schema(description = "유저 닉네임", example = "불타는 망고")
  private final String nickname;

  @Schema(description = "이미지 url")
  private final String profileUrl;

  @Schema(description = "연결 적합도", example = "75")
  private final float matchingScore;

  public GetUserProfileResponseDto(Long userId, int age, Gender gender, String nickname, String profileUrl, Float matchingScore) {
    this.userId = userId;
    this.age = age;
    this.gender = gender;
    this.nickname = nickname;
    this.profileUrl = profileUrl;
    this.matchingScore = matchingScore;
  }
}
