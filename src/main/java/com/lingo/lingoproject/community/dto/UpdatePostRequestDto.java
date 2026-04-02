package com.lingo.lingoproject.community.dto;

import java.util.List;

public record UpdatePostRequestDto(
    String title,
    String content,
    String topic,
    List<UpdatePostImageRequestDto> imagelist
) {

}
