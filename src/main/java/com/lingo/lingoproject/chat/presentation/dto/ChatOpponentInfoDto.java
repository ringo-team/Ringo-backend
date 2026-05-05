package com.lingo.lingoproject.chat.presentation.dto;

import com.lingo.lingoproject.shared.domain.model.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

@Builder
public record ChatOpponentInfoDto(
    @Schema(description = "유저 id", example = "35")
    Long userId,
    @Schema(description = "프로필 url")
    String profileUrl,
    @Schema(description = "닉네임", example = "불타는 망고")
    String nickname,
    @Schema(description = "해시태그", example = "[\"운동\", \"건강\"]")
    List<String> hashtag
) {

  public static ChatOpponentInfoDto from(User opponent) {
    return ChatOpponentInfoDto.builder()
        .profileUrl(opponent.getProfile().getImageUrl())
        .userId(opponent.getId())
        .nickname(opponent.getNickname())
        .hashtag(opponent.getUserHashtags())
        .build();
  }

  public static ChatOpponentInfoDto withdrawn() {
    return ChatOpponentInfoDto.builder()
        .profileUrl(null)
        .userId(null)
        .nickname("알 수 없음")
        .hashtag(null)
        .build();
  }
}
