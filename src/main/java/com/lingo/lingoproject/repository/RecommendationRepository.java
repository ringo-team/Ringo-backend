package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.Recommendation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

  List<Recommendation> findAllByCategoryContainingIgnoreCase(String category);
}
