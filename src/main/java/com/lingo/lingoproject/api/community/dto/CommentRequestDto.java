package com.lingo.lingoproject.api.community.dto;

import jakarta.validation.constraints.NotBlank;

public record CommentRequestDto(
    @NotBlank Long postId,
    @NotBlank String content
) {

}
