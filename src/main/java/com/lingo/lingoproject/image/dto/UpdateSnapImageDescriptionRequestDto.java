package com.lingo.lingoproject.image.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateSnapImageDescriptionRequestDto(
    @Schema(description = "스냅 사진 설명")
    String description
) {

}
