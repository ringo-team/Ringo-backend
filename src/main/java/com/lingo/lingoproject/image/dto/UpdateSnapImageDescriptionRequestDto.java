package com.lingo.lingoproject.image.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateSnapImageDescriptionRequestDto(
    @NotNull @Schema(description = "스냅 사진 id", example = "103")
    Long snapImageId,

    @Schema(description = "스냅 사진 설명")
    String description
) {

}
