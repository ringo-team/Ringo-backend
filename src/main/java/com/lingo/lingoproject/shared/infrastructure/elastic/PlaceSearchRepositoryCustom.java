package com.lingo.lingoproject.shared.infrastructure.elastic;

import com.lingo.lingoproject.shared.domain.elastic.PlaceDocument;
import java.util.List;

public interface PlaceSearchRepositoryCustom {
  List<PlaceDocument> searchByKeyword(String keyword);
}
