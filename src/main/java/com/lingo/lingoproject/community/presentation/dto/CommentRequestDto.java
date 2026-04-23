package com.lingo.lingoproject.community.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CommentRequestDto(
    @NotNull Long postId,
    @NotBlank String content
) {

}
