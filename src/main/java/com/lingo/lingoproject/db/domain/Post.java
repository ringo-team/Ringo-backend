package com.lingo.lingoproject.db.domain;

import com.lingo.lingoproject.db.domain.enums.PostTopic;
import com.lingo.lingoproject.common.utils.Timestamp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;

@Entity
@Table(name = "POSTS")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@DynamicInsert
@Getter@Setter
public class Post extends Timestamp {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "recommendation_id")
  private Recommendation recommendation;

  @Column(length = 100)
  private String title;
  @Lob
  private String content;

  @Enumerated(value = EnumType.STRING)
  private PostTopic topic;

  @ColumnDefault(value = "0")
  @Builder.Default
  private int likeCount = 0;

  @ColumnDefault(value = "0")
  @Builder.Default
  private int commentCount = 0;

  @ManyToOne
  @JoinColumn(name = "author_id")
  private User author;
}
