package com.lingo.lingoproject.image.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record GetImageUrlResponseDto(
    @Schema(description = "이미지 url")
    String imageUrl,
    @Schema(description = "이미지 id", example = "5")
    Long imageId
) {
}
