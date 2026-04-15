package com.lingo.lingoproject.api.community.dto;

import java.util.List;

public record UpdatePostRequestDto(
    String title,
    String content,
    String topic,
    List<UpdatePostImageRequestDto> imagelist
) {

}
