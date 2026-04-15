package com.lingo.lingoproject.db.domain;

import com.lingo.lingoproject.common.utils.Timestamp;
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
public class Profile extends Timestamp {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne
  @JoinColumn(name = "user_id", unique = true)
  private User user;

  private String imageUrl;

  private String description;

  @ColumnDefault(value = "false")
  @Builder.Default
  private boolean isVerified = false;
}
