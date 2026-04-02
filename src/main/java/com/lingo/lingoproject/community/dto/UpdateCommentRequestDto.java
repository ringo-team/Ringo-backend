package com.lingo.lingoproject.community.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateCommentRequestDto(
    @NotBlank String content
) {

}
