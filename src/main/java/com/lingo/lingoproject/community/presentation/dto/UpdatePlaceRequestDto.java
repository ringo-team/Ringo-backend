package com.lingo.lingoproject.community.presentation.dto;

public record UpdatePlaceRequestDto(
    Long id,
    String modifiedName,
    String sigungu,
    String dong,
    String hotplaceCategory,
    String hotplaceSubCategory,
    String[] keywords,
    String type
) {

}
