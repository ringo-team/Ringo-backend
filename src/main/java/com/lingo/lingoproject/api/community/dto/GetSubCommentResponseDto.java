package com.lingo.lingoproject.api.community.dto;

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

}
