package com.lingo.lingoproject.report.presentation.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record GetActiveUserStatResponseDto(
    String metric,
    List<GetDetailStatResponseDto> data
) {
  public record GetDetailStatResponseDto(
    String date,
    int value
  ){}
}
