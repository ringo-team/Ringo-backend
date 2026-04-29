package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.shared.domain.model.Place;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PlaceRepository extends JpaRepository<Place, Long> {

  List<Place> findAllByKeywordContainingIgnoreCase(String keyword);

  List<Place> findAllByType(String type);

  @Modifying
  @Query("update Place p set p.clickCount = p.clickCount + 1 where p.id = :placeId")
  void updatePlaceClickCount(Long clickCount);

  List<Place> findAllByTypeNotNull();
}
