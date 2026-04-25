package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.shared.domain.model.Place;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.domain.model.UserScrapPlace;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserScrapPlaceRepository extends JpaRepository<UserScrapPlace, Long> {

  boolean existsByUserAndPlace(User user, Place place);

  List<UserScrapPlace> findAllByUser(User user);
}
