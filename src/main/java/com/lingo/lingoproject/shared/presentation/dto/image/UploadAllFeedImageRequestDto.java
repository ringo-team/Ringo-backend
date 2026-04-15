package com.lingo.lingoproject.shared.presentation.dto.image;

import java.util.List;
import lombok.Data;

@Data
public class UploadAllFeedImageRequestDto {
  private List<FeedImageDataRequestDto> list;
}
