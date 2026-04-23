package com.lingo.lingoproject.community.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SavePostRequestDto(
    @NotBlank String title,
    @NotBlank String content,
    @NotNull Long userId,
    @NotBlank String category
) {

}
