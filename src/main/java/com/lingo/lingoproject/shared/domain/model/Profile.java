package com.lingo.lingoproject.shared.domain.model;

import com.lingo.lingoproject.shared.utils.Timestamp;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "PROFILES",
    indexes = {
        @Index(name = "idx_profiles_user_id", columnList = "user_id")
    }
)
@DynamicInsert
public class Profile extends Timestamp implements Image{

  public static Profile of(User user, String imageUrl) {
    return Profile.builder()
        .user(user)
        .imageUrl(imageUrl)
        .build();
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne
  @JoinColumn(name = "user_id", unique = true)
  private User user;

  private String imageUrl;

  private String description;

  @Builder.Default
  private int clickCount = 0;

  @Builder.Default
  private int impressionCount = 0;

  @Enumerated(EnumType.STRING)
  @ColumnDefault("'PENDING'")
  @Builder.Default
  private FaceVerify faceVerify = FaceVerify.PENDING;

  @Override
  public User getUser(){
    return user;
  }
}
