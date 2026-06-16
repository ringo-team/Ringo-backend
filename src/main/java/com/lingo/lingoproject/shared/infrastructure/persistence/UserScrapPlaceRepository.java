package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.shared.domain.model.Place;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.domain.model.UserScrapPlace;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserScrapPlaceRepository extends JpaRepository<UserScrapPlace, Long> {

  boolean existsByUserAndPlace(User user, Place place);

  @Query("""
    select distinct usp 
      from UserScrapPlace usp 
        join fetch usp.place p
        left join fetch p.images
    where usp.user = :user
  """)
  List<UserScrapPlace> findAllByUserWithImages(User user);

  void deleteByUserAndPlace(User user, Place place);
}
