package com.lingo.lingoproject.community.dto;

import jakarta.validation.constraints.NotBlank;

public record SavePostRequestDto(
    @NotBlank String title,
    @NotBlank String content,
    @NotBlank Long userId,
    @NotBlank Long recommendationId,
    @NotBlank String topic
) {

}
