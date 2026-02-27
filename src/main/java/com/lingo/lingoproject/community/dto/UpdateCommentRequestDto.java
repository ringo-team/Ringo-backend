package com.lingo.lingoproject.community.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateCommentRequestDto(
    @NotBlank Long commentId,
    @NotBlank String content
) {

}
