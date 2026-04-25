package com.lingo.lingoproject.shared.domain.model;

import com.lingo.lingoproject.community.presentation.dto.UpdatePostRequestDto;
import com.lingo.lingoproject.shared.domain.elastic.PostDocument;
import com.lingo.lingoproject.shared.utils.GenericUtils;
import com.lingo.lingoproject.shared.utils.Timestamp;
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

  public static Post of(User author, String title, String content, PostCategory category) {
    return Post.builder()
        .author(author)
        .title(title)
        .content(content)
        .category(category)
        .build();
  }

  public void updatePost(UpdatePostRequestDto dto){
    PostCategory postCategory = dto.topic() != null
        ? GenericUtils.validateAndReturnEnumValue(PostCategory.values(), dto.topic())
        : null;

    if (dto.title() != null && !dto.title().isBlank()) this.setTitle(dto.title());
    if (dto.content() != null && !dto.content().isBlank()) this.setContent(dto.content());
    if (postCategory != null) this.setCategory(postCategory);
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(length = 100)
  private String title;
  @Lob
  private String content;

  @Column(length = 10)
  private String place;

  @Enumerated(value = EnumType.STRING)
  private PostCategory category;

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
