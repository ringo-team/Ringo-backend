package com.lingo.lingoproject.shared.infrastructure.elastic;

import com.lingo.lingoproject.shared.domain.elastic.PostDocument;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PostSearchRepositoryCustomImpl implements PostSearchRepositoryCustom{

  private final ElasticsearchOperations operations;

  @Override
  public Page<PostDocument> searchKeywordOrPlace(String keyword, String place, Pageable pageable) {

    Query query = NativeQuery.builder()
        .withQuery(q -> q
            .bool(b -> {

              // keyword 조건
              if (keyword != null && !keyword.isBlank()) {
                b.should(s -> s
                    .wildcard(w -> w
                        .field("title")
                        .value("*" + keyword + "*")
                    )
                );
                b.should(s -> s
                    .wildcard(w -> w
                        .field("content")
                        .value("*" + keyword + "*")
                    )
                );
              }

              // place 조건
              if (place != null && !place.isBlank()) {
                b.should(s -> s
                    .match(m -> m
                        .field("place")
                        .query(place)
                    )
                );
              }

              // 아무 조건도 없으면 전체 조회
              if ((keyword == null || keyword.isBlank()) &&
                  (place == null || place.isBlank())) {
                b.must(m -> m.matchAll(ma -> ma));
              }

              return b;
            })
        )
        .withPageable(pageable)
        .build();

    SearchHits<PostDocument> hits =
        operations.search(query, PostDocument.class);

    List<PostDocument> content = hits.getSearchHits().stream()
        .map(SearchHit::getContent)
        .toList();

    return new PageImpl<>(content, pageable, hits.getTotalHits());
  }
}
