package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.shared.domain.model.Matching;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.domain.model.MatchingStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchingRepository extends JpaRepository<Matching, Long> {

  void deleteAllByRequestedUser(User requestedUser);

  void deleteAllByRequestUser(User requestUser);

  boolean existsByRequestUserAndRequestedUserAndMatchingStatus(User requestUser, User requestedUser, MatchingStatus matchingStatus);

  List<Matching> findAllByRequestUser(User requestUser);

  List<Matching> findAllByRequestedUser(User requestedUser);

  Matching findFirstByRequestUserAndRequestedUser(User user1, User user2);

  boolean existsByRequestUserAndRequestedUser(User requestUser, User requestedUser);

  List<Matching> findAllByRequestUserOrRequestedUser(User requestUser, User requestedUser);
}
