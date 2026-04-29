package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.shared.domain.model.Matching;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.domain.model.MatchingStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchingRepository extends JpaRepository<Matching, Long> {

  void deleteAllByRequestedUser(User requestedUser);

  void deleteAllByRequestUser(User requestUser);

  boolean existsByRequestUserAndRequestedUserAndMatchingStatus(User requestUser, User requestedUser, MatchingStatus matchingStatus);

  List<Matching> findAllByRequestUser(User requestUser);

  List<Matching> findAllByRequestedUser(User requestedUser);

  Matching findFirstByRequestUserAndRequestedUser(User user1, User user2);

  boolean existsByRequestUserAndRequestedUser(User requestUser, User requestedUser);

  List<Matching> findAllByRequestUserOrRequestedUser(User requestUser, User requestedUser);

  @Query("SELECT m.id FROM Matching m WHERE m.requestUser = :user AND m.matchingStatus <> com.lingo.lingoproject.shared.domain.model.MatchingStatus.PRE_REQUESTED")
  List<Long> findMatchingIdsByRequestUserExcludingPreRequested(@Param("user") User user);

  @Query("SELECT m.id FROM Matching m WHERE m.requestedUser = :user AND m.matchingStatus <> com.lingo.lingoproject.shared.domain.model.MatchingStatus.PRE_REQUESTED")
  List<Long> findMatchingIdsByRequestedUserExcludingPreRequested(@Param("user") User user);

  @Query("SELECT m.requestedUser.id FROM Matching m WHERE m.requestUser = :user")
  List<Long> findRequestedUserIdsByRequestUser(@Param("user") User user);

  @Query("SELECT m.requestUser.id FROM Matching m WHERE m.requestedUser = :user")
  List<Long> findRequestUserIdsByRequestedUser(@Param("user") User user);
}
