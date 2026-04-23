package com.lingo.lingoproject.shared.infrastructure.elastic;

import com.lingo.lingoproject.shared.domain.elastic.PostDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostSearchRepositoryCustom {
  Page<PostDocument> searchKeywordOrPlace(String keyword, String place, Pageable pageable);

}
