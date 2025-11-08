package com.lingo.lingoproject.match.dto;

import com.lingo.lingoproject.domain.enums.Gender;
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
  private Gender gender;

  @Schema(description = "유저 닉네임", example = "불타는 망고")
  private String nickname;

  @Schema(description = "이미지 url")
  private String profileUrl;

  @Schema(description = "연결 적합도", example = "75")
  private float matchingScore;

  @Schema(description = "해시태그", example = "[\"운동\", \"건강\"]")
  private List<String> hashtags;

  public GetUserProfileResponseDto(Long userId, Integer age, Gender gender, String nickname, String profileUrl, Float matchingScore) {
    this.userId = userId;
    this.age = age;
    this.gender = gender;
    this.nickname = nickname;
    this.profileUrl = profileUrl;
    this.matchingScore = matchingScore;
  }
}
