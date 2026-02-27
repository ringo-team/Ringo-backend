package com.lingo.lingoproject.community.dto;

import java.util.List;

public record SavePostResponseDto(
    Long postId,
    List<SavePostImageResponseDto> imageList,
    String result
) {

}
