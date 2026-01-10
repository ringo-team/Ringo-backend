package com.lingo.lingoproject.image.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateFeedImageDescriptionRequestDto(
    @Schema(description = "피드 사진 설명")
    String description
) {

}
