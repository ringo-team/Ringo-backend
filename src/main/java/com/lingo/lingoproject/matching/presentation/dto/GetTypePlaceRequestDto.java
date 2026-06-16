package com.lingo.lingoproject.matching.presentation.dto;

import com.lingo.lingoproject.community.presentation.dto.GetPlaceDetailResponseDto;
import java.util.List;

public record GetTypePlaceRequestDto(
    String 주제,
    List<GetPlaceDetailResponseDto> 장소_상세정보
) {

}
