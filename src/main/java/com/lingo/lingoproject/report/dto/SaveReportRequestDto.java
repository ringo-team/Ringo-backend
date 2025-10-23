package com.lingo.lingoproject.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SaveReportRequestDto(
    @NotNull
    @Schema(description = "신고자 id", example = "3")
    Long reportUserId,

    @NotNull
    @Schema(description = "신고 받은 자 id", example = "5")
    Long reportedUserId,

    @NotBlank
    String reason
) {

}
