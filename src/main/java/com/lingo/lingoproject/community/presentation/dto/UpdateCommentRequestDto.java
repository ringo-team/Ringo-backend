package com.lingo.lingoproject.community.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateCommentRequestDto(
    @NotBlank String content
) {

}
