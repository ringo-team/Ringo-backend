package com.lingo.lingoproject.match.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record RequestMatchingResponseDto(
    @Schema(description = "매칭 요청이 왔을 때 생성되는 매칭 id", example = "4")
    Long matchingId) {

}
