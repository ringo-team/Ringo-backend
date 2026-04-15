package com.lingo.lingoproject.community.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record CommentRequestDto(
    @NotBlank Long postId,
    @NotBlank String content
) {

}
