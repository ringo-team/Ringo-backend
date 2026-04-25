package com.lingo.lingoproject.matching.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "매칭 요청 정보")
public record MatchingRequestDto(
    @Schema(description = "매칭을 요청하는 사용자 ID", example = "12")
    @NotNull
    Long requestId,
    @Schema(description = "매칭 대상 사용자 ID", example = "45")
    @NotNull
    Long requestedId,
    @NotBlank
    @Schema(description = "연결 요청 메세지", example = "안녕하세요")
    String message
) {

}
