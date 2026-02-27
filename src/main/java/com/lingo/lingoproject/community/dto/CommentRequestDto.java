package com.lingo.lingoproject.community.dto;

import jakarta.validation.constraints.NotBlank;

public record CommentRequestDto(
    @NotBlank Long postId,
    @NotBlank Long userId,
    @NotBlank String content
) {

}
