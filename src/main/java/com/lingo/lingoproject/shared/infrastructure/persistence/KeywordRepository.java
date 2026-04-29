package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.shared.domain.model.Keyword;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KeywordRepository extends JpaRepository<Keyword, Long> {

  List<Keyword> findByKeywordContaining(String keyword);

  int findAllByKeywordContaining(String keyword);

  Keyword findByKeyword(String keyword);

  List<Keyword> findAllByKeywordIn(Collection<String> keywords);
}
