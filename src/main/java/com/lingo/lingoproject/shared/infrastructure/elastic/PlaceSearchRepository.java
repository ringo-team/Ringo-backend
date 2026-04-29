package com.lingo.lingoproject.shared.infrastructure.elastic;

import com.lingo.lingoproject.shared.domain.elastic.PlaceDocument;
import java.util.List;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface PlaceSearchRepository extends ElasticsearchRepository<PlaceDocument, Long>, PlaceSearchRepositoryCustom {

  List<PlaceDocument> findAllByKeywordContaining(String keyword);
}
