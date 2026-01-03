package com.lingo.lingoproject.image.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class FeedImageDataRequestDto {
  @Schema(format = "binary", description = "이미지 파일")
  private MultipartFile image;

  @Schema(description = "이미지 설명")
  private String content;
}
