package com.lingo.lingoproject.community.presentation.dto;

import com.lingo.lingoproject.matching.presentation.dto.GetTypePlaceRequestDto;
import java.util.List;

public record GetPlaceResponseDto(
    List<GetPlaceDetailResponseDto> individualPlaces,
    List<GetTypePlaceRequestDto> commonPlaces,
    String result
) {

}
