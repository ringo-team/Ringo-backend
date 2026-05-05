package com.lingo.lingoproject.shared.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Table(name = "SNAP_IMAGES")
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedImage implements Image{

  public static FeedImage of(User user, String imageUrl, String description) {
    return FeedImage.builder()
        .user(user)
        .imageUrl(imageUrl)
        .description(description)
        .build();
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne
  @JoinColumn(name = "user_id")
  public User user;

  public String imageUrl;

  @Lob
  public String description;

  @Override
  public User getUser(){
    return user;
  }
}
