package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.Matching;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.domain.enums.MatchingStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchingRepository extends JpaRepository<Matching, Long> {

  List<Matching> findByRequestUserAndMatchingStatus(User requestUser, MatchingStatus matchingStatus);

  List<Matching> findByRequestedUserAndMatchingStatus(User requestedUser, MatchingStatus matchingStatus);
}
