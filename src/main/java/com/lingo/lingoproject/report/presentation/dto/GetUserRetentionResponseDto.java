package com.lingo.lingoproject.report.presentation.dto;

import lombok.Builder;

@Builder
public record GetUserRetentionResponseDto(
    String startDate,
    String endDate,
    int retentionCount,
    float retentionRate,
    float newUserRate
){

}
