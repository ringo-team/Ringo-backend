package com.lingo.lingoproject.community.dto;

public record CreateSubCommentRequestDto(
    Long commentId,
    String content
) {

}
