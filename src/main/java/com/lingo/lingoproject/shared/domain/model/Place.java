package com.lingo.lingoproject.shared.domain.model;

import com.lingo.lingoproject.community.presentation.dto.GetPlaceDetailResponseDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Entity
@Table(name = "PLACES")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Setter
public class Place {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  // 카페, 놀거리, 맛집/술집
  @Enumerated(EnumType.STRING)
  private PostCategory category;

  @OneToMany(mappedBy = "place")
  private List<PlaceImage> images;

  @Lob
  private String description;

  // 카테고리1,카테고리2,카테고리3
  private String keyword;

  private String phoneNumber;

  @Column(length = 10)
  private String province; // 도/시
  @Column(length = 10)
  private String city;     // 시/군
  @Column(length = 10)
  private String district; // 구
  @Column(length = 10)
  private String neighbor; // 동

  private String detailAddress;

  private String type;

  @Builder.Default
  private long clickCount = 0L;

  public GetPlaceDetailResponseDto createPlaceDetailDto(boolean isScrap){
    List<String> keywords = keyword != null ? Arrays.stream(this.keyword.split(",")).toList() : null;
    List<String> profileUrls = convertPlaceImagesToUrlList(this.images);
    if (!profileUrls.isEmpty()) log.info("profile url");
    return GetPlaceDetailResponseDto.builder()
        .id(this.id)
        .placeName(this.name)
        .category(this.category.toString())
        .profileUrl(profileUrls)
        .description(this.description)
        .keyword(keywords)
        .phoneNumber(this.phoneNumber)
        .city(this.city)
        .district(this.district)
        .neighbor(this.neighbor)
        .type(this.type)
        .isScrap(isScrap)
        .build();
  }

  private List<String> convertPlaceImagesToUrlList(List<PlaceImage> places){
    return places.stream()
        .map(PlaceImage::getImage)
        .toList();
  }

}
