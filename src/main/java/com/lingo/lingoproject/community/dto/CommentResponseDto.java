package com.lingo.lingoproject.community.dto;

import jakarta.validation.constraints.NotBlank;

public record CommentResponseDto(
    @NotBlank Long commentId,
    @NotBlank String result
) {

}
