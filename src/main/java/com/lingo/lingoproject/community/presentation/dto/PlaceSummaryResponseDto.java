package com.lingo.lingoproject.community.presentation.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record PlaceSummaryResponseDto(
    String name,
    String address,
    String district,
    String neighbor,
    String addressCategory,
    String addressSubcategory,
    String category,
    List<String> imageUrls,
    List<String> keyword,
    String type
) {

}
