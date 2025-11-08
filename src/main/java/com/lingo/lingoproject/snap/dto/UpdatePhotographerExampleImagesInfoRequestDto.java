package com.lingo.lingoproject.snap.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdatePhotographerExampleImagesInfoRequestDto(
    @NotNull @Schema(description = "예시 이미지 id", example = "5")
    Long imageId,

    @Schema(description = "촬영 장소", example = "뚝섬한강공원")
    String snapLocation,

    @Schema(description = "촬영 시기", example = "2025-05-28")
    String snapDate
) {

}
