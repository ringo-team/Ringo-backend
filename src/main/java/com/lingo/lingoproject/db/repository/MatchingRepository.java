package com.lingo.lingoproject.db.repository;

import com.lingo.lingoproject.db.domain.Matching;
import com.lingo.lingoproject.db.domain.User;
import com.lingo.lingoproject.db.domain.enums.MatchingStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchingRepository extends JpaRepository<Matching, Long> {

  void deleteAllByRequestedUser(User requestedUser);

  void deleteAllByRequestUser(User requestUser);

  boolean existsByRequestUserAndRequestedUserAndMatchingStatus(User requestUser, User requestedUser, MatchingStatus matchingStatus);

  List<Matching> findAllByRequestUser(User requestUser);

  List<Matching> findAllByRequestedUser(User requestedUser);

  Matching findFirstByRequestUserAndRequestedUser(User user1, User user2);
}
