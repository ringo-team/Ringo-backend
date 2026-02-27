package com.lingo.lingoproject.community.dto;

import java.util.List;

public record UpdatePostResponseDto(
    List<UpdatePostImageResponseDto> imageList,
    String result
) {

}
