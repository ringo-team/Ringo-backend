package com.lingo.lingoproject.community.presentation.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SaveParsedPlaceRequest(
  Long id,
  String category,
  String city,
  String district,
  String keyword,
  String name,
  String neighbor,
  String type,
  String address_category,
  String address_subcategory
) {
}
