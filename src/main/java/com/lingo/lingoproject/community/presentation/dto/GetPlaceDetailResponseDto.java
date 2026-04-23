package com.lingo.lingoproject.community.presentation.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record GetPlaceDetailResponseDto(
    Long id,
    String placeName,
    String category,
    List<String> profileUrl,
    String description,
    List<String> keyword,
    String phoneNumber,
    String city,
    String district,
    String neighbor,
    String type,
    boolean isScrap
) {

}
