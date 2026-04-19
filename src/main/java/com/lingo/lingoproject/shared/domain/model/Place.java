package com.lingo.lingoproject.shared.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "PLACES")
public class Place {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  // 카페, 놀거리, 맛집/술집
  private String category;

  private String profileUrl;

  @Lob
  private String description;

  // 카테고리1,카테고리2,카테고리3
  private String keyword;

  @Column(length = 10)
  private String province; // 도/시
  @Column(length = 10)
  private String city;     // 시/군
  @Column(length = 10)
  private String district; // 구
  @Column(length = 10)
  private String neighbor; // 동

  @Column(length = 10)
  private String latitude;  // 위도
  @Column(length = 10)
  private String longitude; // 경도



}
