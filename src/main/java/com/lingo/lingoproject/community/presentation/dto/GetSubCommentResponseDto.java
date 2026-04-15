package com.lingo.lingoproject.community.presentation.dto;

import com.lingo.lingoproject.shared.domain.model.SubComment;
import lombok.Builder;

@Builder
public record GetSubCommentResponseDto(
    Long subCommentId,
    String content,
    Long userId,
    String userProfileUrl,
    String userNickname,
    String updatedAt
) {

  public static GetSubCommentResponseDto from(SubComment subComment) {
    return GetSubCommentResponseDto.builder()
        .subCommentId(subComment.getId())
        .content(subComment.getContent())
        .userId(subComment.getUser().getId())
        .userNickname(subComment.getUser().getNickname())
        .userProfileUrl(subComment.getUser().getProfile().getImageUrl())
        .updatedAt(subComment.getUpdatedAt().toString())
        .build();
  }
}
