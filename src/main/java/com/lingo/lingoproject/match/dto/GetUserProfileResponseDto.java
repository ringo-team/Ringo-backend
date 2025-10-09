package com.lingo.lingoproject.match.dto;

import com.lingo.lingoproject.domain.enums.Gender;
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
  private final Long userId;
  private final int age;
  private final Gender gender;
  private final String nickname;
  private final String profileUrl;

  public GetUserProfileResponseDto(Long userId, int age, Gender gender, String nickname, String profileUrl) {
    this.userId = userId;
    this.age = age;
    this.gender = gender;
    this.nickname = nickname;
    this.profileUrl = profileUrl;
  }
}
