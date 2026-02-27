package com.lingo.lingoproject.community.dto;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record GetCommentResponseDto(
    Long commentId,
    String userProfileUrl,
    String userName,
    String content,
    LocalDateTime updatedAt,
    String result
) {

}
