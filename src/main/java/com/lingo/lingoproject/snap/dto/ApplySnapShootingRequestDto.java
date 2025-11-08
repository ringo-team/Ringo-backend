package com.lingo.lingoproject.snap.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ApplySnapShootingRequestDto(
    @NotNull @Schema(description = "스냅 촬영 기사 id", example = "103")
    Long photographerId,

    @NotNull @Schema(description = "스냅 신청자 id", example = "13")
    Long userId,

    @NotBlank @Schema(description = "스냅 촬영 시간", example = "2025-10-25 11:00:00")
    String snapStartedAt,

    @NotNull @Schema(description = "스냅 촬영 소요 시간", example = "90")
    Integer snapDuration,

    @NotBlank @Schema(description = "촬영 위치", example = "뚝섬한강공원")
    String snapLocation,

    @NotBlank @Schema(description = "만나는 위치", example = "자양역 1번 출구")
    String meetingLocation,

    @NotBlank @Schema(description = "촬영 키워드", example = "하늘")
    String keyword,

    @NotBlank @Schema(description = "촬영 추가 요구사항", example = "멋지게 찍어주세요")
    String extraRequest
) {

}
