package com.lingo.lingoproject.api.community.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateCommentRequestDto(
    @NotBlank String content
) {

}
