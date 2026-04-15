package com.lingo.lingoproject.api.image.dto;

import java.util.List;
import lombok.Data;

@Data
public class UploadAllFeedImageRequestDto {
  private List<FeedImageDataRequestDto> list;
}
