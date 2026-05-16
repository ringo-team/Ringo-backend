package com.lingo.lingoproject.community.presentation.dto;

import com.lingo.lingoproject.shared.domain.model.Post;
import com.lingo.lingoproject.shared.domain.model.Profile;
import com.lingo.lingoproject.shared.domain.model.User;
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
    boolean isLike,
    Integer commentCount,
    String category,
    List<GetPostImageResponseDto> images,
    String createdAt,
    String updatedAt,
    String result
) {

  public static GetPostResponseDto from(Post post, boolean isLike, List<GetPostImageResponseDto> images) {
    User author = post.getAuthor() != null ? post.getAuthor() : null;
    String authorProfileUrl = author != null ?
        (author.getProfile() != null ? author.getProfile().getImageUrl() : null)
        : null;
    String authorNickname = author != null ? author.getNickname() : null;
    String createdAt = post.getCreatedAt() != null ? post.getCreatedAt().toString() : null;
    String updatedAt = post.getUpdatedAt() != null ? post.getUpdatedAt().toString() : createdAt;
    return GetPostResponseDto.builder()
        .postId(post.getId())
        .title(post.getTitle())
        .content(post.getContent())
        .authorProfileUrl(authorProfileUrl)
        .authorName(authorNickname)
        .likeCount(post.getLikeCount())
        .isLike(isLike)
        .commentCount(post.getCommentCount())
        .category(post.getCategory().toString())
        .images(images)
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .result(ErrorCode.SUCCESS.getCode())
        .build();
  }
}
