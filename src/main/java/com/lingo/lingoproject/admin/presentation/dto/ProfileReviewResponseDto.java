package com.lingo.lingoproject.admin.presentation.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record ProfileReviewResponseDto(
    String id,
    String userId,
    String nickname,
    List<String> imageUrls,
    String submittedAt,
    String status
) {
}
