package com.lingo.lingoproject.match.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record GetMatchingRequestMessageResponseDto(
    @Schema(description = "응답 결과", example = "0000")
    String result,
    @Schema(description = "메세지", example = "안녕하세요")
    String message
) {

}
