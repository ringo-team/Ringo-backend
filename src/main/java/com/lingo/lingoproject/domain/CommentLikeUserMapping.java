package com.lingo.lingoproject.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "COMMENT_LIKE_USER_MAPPINGS",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "comment_like_user_mapping_unique_contraint_comment_user",
            columnNames = {"comment_id", "user_id"}
        )
    }
)
public class CommentLikeUserMapping {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "comment_id")
  private Comment comment;

  @ManyToOne
  @JoinColumn(name = "user_id")
  private User user;
}
