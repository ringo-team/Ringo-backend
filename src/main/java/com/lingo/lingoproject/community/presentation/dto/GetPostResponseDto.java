package com.lingo.lingoproject.community.presentation.dto;

import com.lingo.lingoproject.shared.domain.model.Post;
import com.lingo.lingoproject.shared.exception.ErrorCode;
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

  public static GetPostResponseDto from(Post post, List<GetPostImageResponseDto> images) {
    return GetPostResponseDto.builder()
        .postId(post.getId())
        .title(post.getTitle())
        .content(post.getContent())
        .authorProfileUrl(post.getAuthor().getProfile().getImageUrl())
        .authorName(post.getAuthor().getNickname())
        .likeCount(post.getLikeCount())
        .commentCount(post.getCommentCount())
        .images(images)
        .updatedAt(post.getUpdatedAt())
        .result(ErrorCode.SUCCESS.getCode())
        .build();
  }
}
