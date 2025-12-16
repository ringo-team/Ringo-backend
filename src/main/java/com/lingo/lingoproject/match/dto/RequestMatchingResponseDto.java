package com.lingo.lingoproject.match.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record RequestMatchingResponseDto(
    @Schema(description = "응답 결과", example = "0000")
    String result,
    @Schema(description = "매칭 요청이 왔을 때 생성되는 매칭 id", example = "4")
    Long matchingId
) {

}
