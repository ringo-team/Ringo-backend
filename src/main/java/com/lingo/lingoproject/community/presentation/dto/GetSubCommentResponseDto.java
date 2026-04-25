package com.lingo.lingoproject.community.presentation.dto;

import com.lingo.lingoproject.shared.domain.model.Comment;
import com.lingo.lingoproject.shared.domain.model.SubComment;
import java.util.List;
import lombok.Builder;

@Builder
public record GetSubCommentResponseDto(
    Long subCommentId,
    String content,
    Long userId,
    String userProfileUrl,
    String userNickname,
    int commentLikeCount,
    boolean isLike,
    String updatedAt
) {

  public static GetSubCommentResponseDto from(Comment subComment, boolean isLike) {
    return GetSubCommentResponseDto.builder()
        .subCommentId(subComment.getId())
        .content(subComment.getContent())
        .userId(subComment.getUser().getId())
        .userNickname(subComment.getUser().getNickname())
        .userProfileUrl(subComment.getUser().getProfile().getImageUrl())
        .updatedAt(subComment.getUpdatedAt() == null ? subComment.getCreatedAt().toString() : subComment.getUpdatedAt().toString())
        .isLike(isLike)
        .commentLikeCount(subComment.getLikeCount())
        .build();
  }
}
