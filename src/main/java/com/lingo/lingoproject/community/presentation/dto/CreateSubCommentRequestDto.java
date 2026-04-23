package com.lingo.lingoproject.community.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSubCommentRequestDto(
    @NotNull Long commentId,
    @NotBlank String content
) {

}
