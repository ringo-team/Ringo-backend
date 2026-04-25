package com.lingo.lingoproject.shared.domain.model;

import com.lingo.lingoproject.shared.utils.Timestamp;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;

@Entity
@Table(name = "COMMENTS")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter @Setter
@DynamicInsert
public class Comment extends Timestamp {

  public static Comment of(Post post, User user, String content) {
    return Comment.builder()
        .post(post)
        .user(user)
        .content(content)
        .build();
  }

  public static Comment of(Post post, Comment parent, User user, String content){
    return Comment.builder()
        .post(post)
        .parentComment(parent)
        .user(user)
        .content(content)
        .build();
  }

  public boolean hasParent(){
    return parentComment != null;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "post_id")
  private Post post;

  @ManyToOne
  @JoinColumn(name = "user_id")
  private User user;

  @ColumnDefault(value = "0")
  @Builder.Default
  private int likeCount = 0;

  @Lob
  private String content;

  @ManyToOne
  @JoinColumn(name = "parent_comment_id")
  private Comment parentComment;
}
