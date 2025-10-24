package com.lingo.lingoproject.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record GetReportInfoRequestDto(
    @Schema(description = "신고자 혹은 피신고자의 id", example = "5")
    Long userId,

    @Schema(description = "신고자 상태", example = "PENDING",
    allowableValues = {"PENDING", "PERMANENT_ACCOUNT_SUSPENSION", "SEVERE_ACCOUNT_SUSPENSION",
        "RISKY_ACCOUNT_SUSPENSION", "MINOR_ACCOUNT_SUSPENSION", "WARNING", "LEGAL_REVIEW", "INNOCENT_REPORT"})
    String reportedUserStatus,

    @Schema(description = "신고 강도", example = "PENDING",
    allowableValues = {"PENDING", "MINOR", "WARNING", "SEVERE", "ILLEGAL"})
    String reportIntensity,

    @Schema(description = "정렬", example = "CREATED_AT_DESC",
    allowableValues = {"CREATED_AT_DESC", "CREATED_AT_ASC", "STATUS_DESC",
        "STATUS_ASC", "INTENSITY_DESC", "INTENSITY_ASC"})
    String ordering,

    @Schema(description = "신고 조회 시작일", example = "2025-08-27")
    String startedAt,

    @Schema(description = "신고 조회 마감일", example = "2025-09-02")
    String finishedAt
) {

}
