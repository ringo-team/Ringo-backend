package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.shared.domain.model.Place;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PlaceRepository extends JpaRepository<Place, Long> {

  List<Place> findAllByType(String type);

  @Modifying
  @Query("update Place p set p.clickCount = p.clickCount + 1 where p.id = :placeId")
  void updatePlaceClickCount(Long placeId);

  @Query("select p from Place p order by p.clickCount limit 5")
  List<Place> 가장_많이_클릭한_컨텐츠_상위_5개만_조회();

  List<Place> findAllByTypeNotNull();

  List<Place> findAllByIdIn(Collection<Long> ids);
}
