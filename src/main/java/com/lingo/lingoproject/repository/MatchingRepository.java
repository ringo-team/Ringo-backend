package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.Matching;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.domain.enums.MatchingStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchingRepository extends JpaRepository<Matching, Long> {

  List<Matching> findByRequestUserAndMatchingStatus(User requestUser, MatchingStatus matchingStatus);

  List<Matching> findByRequestedUserAndMatchingStatus(User requestedUser, MatchingStatus matchingStatus);

  @Modifying
  @Query("update Matching m set m.requestedUser = null where m.requestedUser = :user")
  void disconnectRelationWithUserByInitRequestedUser(@Param("user") User user);

  @Modifying
  @Query("update Matching m set m.requestUser = null where m.requestUser = :user")
  void disconnectRelationWithUserByInitRequestUser(@Param("user") User user);
}
