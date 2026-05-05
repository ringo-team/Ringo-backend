package com.lingo.lingoproject.community.presentation.dto;

import com.lingo.lingoproject.shared.domain.model.PostImage;

public record UpdatePostImageResponseDto(
    Long imageId,
    String imageUrl
) {
  public static UpdatePostImageResponseDto from(PostImage image){
    return new UpdatePostImageResponseDto(image.getId(), image.getImageUrl());
  }
}
