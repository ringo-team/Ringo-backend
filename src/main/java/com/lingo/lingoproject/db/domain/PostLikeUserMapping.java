package com.lingo.lingoproject.db.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "POST_LIKE_USER_MAPPINGS",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "post_like_user_mapping_unique_contraints_post_user",
            columnNames = {"post_id", "user_id"}
        )
    }
)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostLikeUserMapping {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "post_id")
  private Post post;

  @ManyToOne
  @JoinColumn(name = "user_id")
  private User user;
}
