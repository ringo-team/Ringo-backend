package com.lingo.lingoproject.chat.presentation.dto;

import com.lingo.lingoproject.shared.domain.model.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

@Builder
public record GetChatroomMemberInfoResponseDto(
    @Schema(description = "유저 id", example = "35")
    Long userId,
    @Schema(description = "프로필 url")
    String profileUrl,
    @Schema(description = "닉네임", example = "불타는 망고")
    String nickname,
    @Schema(description = "해시태그", example = "[\"운동\", \"건강\"]")
    List<String> hashtag
) {

  public static GetChatroomMemberInfoResponseDto from(User opponent, List<String> hashtags) {
    return GetChatroomMemberInfoResponseDto.builder()
        .profileUrl(opponent.getProfile().getImageUrl())
        .userId(opponent.getId())
        .nickname(opponent.getNickname())
        .hashtag(hashtags)
        .build();
  }

  public static GetChatroomMemberInfoResponseDto withdrawn() {
    return GetChatroomMemberInfoResponseDto.builder()
        .profileUrl(null)
        .userId(null)
        .nickname("알 수 없음")
        .hashtag(null)
        .build();
  }
}
