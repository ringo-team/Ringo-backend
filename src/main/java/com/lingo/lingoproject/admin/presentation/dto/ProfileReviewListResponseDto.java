package com.lingo.lingoproject.admin.presentation.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record ProfileReviewListResponseDto(
    List<ProfileReviewResponseDto> content,
    int page,
    int size,
    long totalElements,
    long totalPages
) {

}
