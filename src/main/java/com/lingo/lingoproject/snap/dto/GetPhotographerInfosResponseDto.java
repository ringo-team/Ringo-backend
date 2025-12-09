package com.lingo.lingoproject.snap.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@AllArgsConstructor
@Data
public class GetPhotographerInfosResponseDto {
  private Long photographerId;
  private String profileUrl;
  private String content;
  private String instagramId;
  List<GetImageInfoRequestDto> images;

  @Data
  public static class GetImageInfoRequestDto {

    String imageUrl;
    String snapLocation;
    String snapDate;

    public GetImageInfoRequestDto(String imageUrl, String snapLocation, LocalDate snapDate) {
      this.imageUrl = imageUrl;
      this.snapLocation = snapLocation;
      this.snapDate = snapDate.toString();
    }
  }
}
