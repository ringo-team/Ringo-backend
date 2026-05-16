package com.lingo.lingoproject.community.presentation.dto;

import java.util.Map;

public record InputStatusResponseDto(
    int rate,
    Map<String, Boolean> number
) {

}
