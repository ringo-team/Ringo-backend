package com.lingo.lingoproject.community.presentation.dto;

import java.util.List;

public record SavePostResponseDto(
    Long postId,
    List<SavePostImageResponseDto> imageList,
    String result
) {

}
