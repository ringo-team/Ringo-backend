package com.lingo.lingoproject.community.presentation.dto;

import java.util.List;

public record GetPlaceResponseDto(
    List<GetPlaceDetailResponseDto> individualPlaces,
    List<GetPlaceDetailResponseDto> commonPlaces,
    String result
) {

}
