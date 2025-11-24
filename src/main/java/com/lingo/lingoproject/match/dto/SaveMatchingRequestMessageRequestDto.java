package com.lingo.lingoproject.match.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record SaveMatchingRequestMessageRequestDto(
    @NotBlank
    @Schema(description = "연결 요청 메세지", example = "안녕하세요")
    String message
) {

}
