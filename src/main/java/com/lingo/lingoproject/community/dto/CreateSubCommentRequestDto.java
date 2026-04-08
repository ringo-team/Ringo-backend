package com.lingo.lingoproject.community.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateSubCommentRequestDto(
    @NotBlank Long commentId,
    @NotBlank String content
) {

}
