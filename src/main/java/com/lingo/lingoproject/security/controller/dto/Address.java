package com.lingo.lingoproject.security.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record Address(
    @Schema(description = "시/도", example = "경기도")
    @NotBlank
    String city,

    @Schema(description = "시/군/구", example = "성남시")
    @NotBlank
    String district
) {

}
