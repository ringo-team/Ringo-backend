package com.lingo.lingoproject.match.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "매칭 요청 정보")
public record MatchingRequestDto(
    @Schema(description = "매칭을 요청하는 사용자 ID", example = "12")
    Long requestId,
    @Schema(description = "매칭 대상 사용자 ID", example = "45")
    Long requestedId) {

}
