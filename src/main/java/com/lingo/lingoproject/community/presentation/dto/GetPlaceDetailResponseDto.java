package com.lingo.lingoproject.community.presentation.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetPlaceDetailResponseDto{
  private Long id;
  private String placeName;
  private String category;
  private List<String> profileUrl;
  private String description;
  private List<String> keyword;
  private String phoneNumber;
  private String city;
  private String district;
  private String neighbor;
  private String detailAddress;
  private String type;
  private Boolean isScrap;
}
