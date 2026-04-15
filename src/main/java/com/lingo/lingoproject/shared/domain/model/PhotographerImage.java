package com.lingo.lingoproject.shared.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "PHOTOGRAPHER_IMAGES")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
public class PhotographerImage {

  public static PhotographerImage of(User photographer, String imageUrl) {
    return PhotographerImage.builder()
        .photographer(photographer)
        .imageUrl(imageUrl)
        .build();
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "photographer_id")
  private User photographer;

  private String imageUrl;
  private String snapLocation;
  private LocalDate snapDate;
}
