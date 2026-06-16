package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.shared.domain.model.Place;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaceRepository extends JpaRepository<Place, Long> {

  @Query(value = "select p.id from Place p order by p.clickCount desc")
  Page<Long> findPlaceIdOrderByClickCount(Pageable pageable);

  @Query(value = "select distinct p from Place p join fetch p.images where p.id in :ids")
  List<Place> findPlaceByIdsWithImages(@Param(value = "ids") List<Long> ids);

  @Query(value = "select distinct p from Place p join fetch p.images where p.type = :type")
  List<Place> findAllByTypeWithImages(@Param(value = "type") String type);

  @Query(value = "select * from places", nativeQuery = true)
  List<Place> findAllPlaces();

  @Query(value = "select distinct p from Place p join fetch p.images where p.id = :placeId")
  Optional<Place> findByIdWithImages(@Param(value = "placeId") Long placeId);

  @Modifying
  @Query("update Place p set p.clickCount = p.clickCount + 1 where p.id = :placeId")
  void updatePlaceClickCount(Long placeId);

  @Query(value = "select distinct p from Place p join fetch p.images order by p.clickCount limit 5")
  List<Place> 가장_많이_클릭한_컨텐츠_상위_5개만_조회();

  List<Place> findAllByTypeNotNull();

  @Query("select p from Place p join fetch p.images where p.id in :ids")
  List<Place> findAllByIdInWithImages(Collection<Long> ids);
}
