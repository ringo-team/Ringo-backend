package com.lingo.lingoproject.shared.presentation.dto.image;

import io.swagger.v3.oas.annotations.media.Schema;

public record GetFeedImageInfoResponseDto(
    @Schema(description = "응답 결과", example = "0000")
    String result,
    @Schema(description = "이미지 url")
    String imageUrl,
    @Schema(description = "이미지 id", example = "5")
    Long imageId,
    @Schema(description = "이미지 설명", example = "친구가 찍어줬어요")
    String description
){

}
