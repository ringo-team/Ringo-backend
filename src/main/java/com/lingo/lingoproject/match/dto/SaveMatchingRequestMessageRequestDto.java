package com.lingo.lingoproject.match.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SaveMatchingRequestMessageRequestDto(
    @NotNull
    @Schema(description = "매칭 id", example = "3")
    Long matchingId,
    @NotBlank
    @Schema(description = "연결 요청 메세지", example = "안녕하세요")
    String message
) {

}
