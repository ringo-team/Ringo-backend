package com.lingo.lingoproject.report.presentation.dto;

import lombok.Builder;

@Builder
public record GetStatOverviewResponseDto(
    String date,
    int activeSessionCount,
    int activeSessionDelta,
    int dau,
    int dauDelta,
    int newSignups,
    int newSignupsDelta,
    int avgSessionDurationSeconds,
    int avgSessionDurationDelta,
    GenderRatio genderRatio
) {
  public record  GenderRatio(
      float malePct,
      float femalePct
  ){

  }
}
