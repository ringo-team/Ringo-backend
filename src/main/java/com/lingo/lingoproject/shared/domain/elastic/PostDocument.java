package com.lingo.lingoproject.shared.domain.elastic;


import com.lingo.lingoproject.community.domain.event.PostCreatedEvent;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Document;

@Document(indexName = "posts")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class PostDocument {
  @Id
  private Long id;

  private String title;
  private String content;

  private String place;

  public static PostDocument from(PostCreatedEvent event){
    return PostDocument.builder()
        .id(event.getPostId())
        .title(event.getTitle())
        .content(event.getContent())
        .place(event.getPlace())
        .build();
  }

}
