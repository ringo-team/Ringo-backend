package com.lingo.lingoproject.community.presentation.dto;

import com.lingo.lingoproject.shared.domain.model.Comment;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;

@Builder
public record GetCommentResponseDto(
    @Schema(description = "댓글 id") Long commentId,
    @Schema(description = "유저의 프로필 url") String userProfileUrl,
    @Schema(description = "유저 Id") Long userId,
    @Schema(description = "유저 닉네임") String userNickname,
    @Schema(description = "댓글 내용") String content,
    @Schema(description = "업데이트 날짜") String updatedAt,
    @Schema(description = "댓글 좋아요 개수") int commentLikeCount,
    @Schema(description = "유저 좋아요 여부") boolean isLike,
    List<GetSubCommentResponseDto> subComments,
    @Schema(description = "응답 결과") String result
) {

  public static GetCommentResponseDto from(Comment comment, boolean isLike) {
    return GetCommentResponseDto.builder()
        .commentId(comment.getId())
        .userId(comment.getUser().getId())
        .userProfileUrl(comment.getUser().getProfile().getImageUrl())
        .userNickname(comment.getUser().getNickname())
        .content(comment.getContent())
        .updatedAt(comment.getUpdatedAt() == null ? comment.getCreatedAt().toString() : comment.getUpdatedAt().toString())
        .subComments(new ArrayList<>())
        .commentLikeCount(comment.getLikeCount())
        .isLike(isLike)
        .result(ErrorCode.SUCCESS.getCode())
        .build();
  }

  public void addSubCommentDto(Comment comment, boolean isLike){
    GetSubCommentResponseDto dto = GetSubCommentResponseDto.from(comment, isLike);
    this.subComments.add(dto);
  }
}
