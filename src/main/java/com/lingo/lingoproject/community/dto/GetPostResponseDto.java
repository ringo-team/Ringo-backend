package com.lingo.lingoproject.community.dto;

import java.time.LocalDateTime;
import java.util.List;
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
    List<GetPostImageResponseDto> images,
    LocalDateTime updatedAt,
    String result
) {

}
