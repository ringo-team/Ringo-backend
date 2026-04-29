package com.lingo.lingoproject.shared.domain.elastic;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.annotations.Setting;

@Document(indexName = "places")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setting(settingPath = "/elasticsearch/settings.json")
@Mapping(mappingPath = "/elasticsearch/place-mapping.json")
public class PlaceDocument {
  @Id
  private Long id;

  private String name;
  private String image;
}
