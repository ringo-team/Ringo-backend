package com.lingo.lingoproject.match.dto;

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
  private Long userId;
  private int age;
  private String nickname;
  private String profileUrl;

  public GetUserProfileResponseDto(Long userId, int age, String nickname, String profileUrl) {
    this.userId = userId;
    this.age = age;
    this.nickname = nickname;
    this.profileUrl = profileUrl;
  }
}
