package com.lingo.lingoproject.community.dto;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record GetPostResponseDto(
    Long postId,
    String title,
    String content,
    String authorName,
    String authorProfileUrl,
    Integer likeCount,
    Integer commentCount,
    String profileUrl,
    LocalDateTime updatedAt,
    String result
) {

}
