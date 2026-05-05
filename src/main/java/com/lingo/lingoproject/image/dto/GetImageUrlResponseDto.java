package com.lingo.lingoproject.image.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record GetImageUrlResponseDto(
    @Schema(description = "응답 결과", example = "0000")
    String result,
    @Schema(description = "이미지 url")
    String imageUrl,
    @Schema(description = "이미지 id", example = "5")
    Long imageId
) {
}
