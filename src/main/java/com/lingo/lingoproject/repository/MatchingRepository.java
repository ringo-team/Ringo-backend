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


  void deleteAllByRequestedUser(User requestedUser);

  void deleteAllByRequestUser(User requestUser);

  boolean existsByRequestUserAndRequestedUserAndMatchingStatus(User requestUser, User requestedUser, MatchingStatus matchingStatus);
}
