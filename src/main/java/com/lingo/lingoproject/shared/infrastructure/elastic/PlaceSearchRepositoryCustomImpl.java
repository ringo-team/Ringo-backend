package com.lingo.lingoproject.shared.infrastructure.elastic;

import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import com.lingo.lingoproject.shared.domain.elastic.PlaceDocument;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PlaceSearchRepositoryCustomImpl implements PlaceSearchRepositoryCustom{

  private final ElasticsearchOperations operations;

  @Override
  public List<PlaceDocument> searchByKeyword(String keyword) {
    Query query = NativeQuery.builder()
        .withQuery(q ->
            q.bool(b -> b

                // 1. 구문 완전 일치 (높은 우선순위)
                .should(s -> s.matchPhrase(m -> m
                    .field("name")
                    .query(keyword)
                    .boost(50f)
                ))

                // 2. 토큰 순서 무관 AND 매칭 (핵심)
                //    "강남 스타벅스" → 강남 AND 스타벅스 모두 포함된 문서
                .should(s -> s.match(m -> m
                    .field("name")
                    .query(keyword)
                    .operator(Operator.And)
                    .boost(30f)
                ))

                // 3. edge_ngram 부분 일치
                .should(s -> s.match(m -> m
                    .field("name")
                    .query(keyword.replaceAll("\\s+", ""))
                    .analyzer("index_analyzer")
                    .boost(10f)
                ))

                // 4. 일부 토큰만 일치해도 검색 (OR)
                .should(s -> s.match(m -> m
                    .field("name")
                    .query(keyword)
                    .boost(5f)
                ))

                .minimumShouldMatch("1")
            )
        )
        .build();
    SearchHits<PlaceDocument> hits =
        operations.search(query, PlaceDocument.class);
    return hits.getSearchHits().stream()
        .map(SearchHit::getContent)
        .toList();
  }
}
