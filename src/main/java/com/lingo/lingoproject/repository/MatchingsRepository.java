package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.Matching;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MatchingsRepository extends JpaRepository<Matching, Long> {
  @Query("select b.id from BlockedUser b join UserEntity u on b.phoneNumber=u.phoneNumber and  u.id = :userId")
  List<String> findBlockUser(String userId);
}
