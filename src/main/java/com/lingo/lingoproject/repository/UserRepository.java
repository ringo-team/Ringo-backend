package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.User;

import com.lingo.lingoproject.domain.enums.Gender;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByEmail(String email);

  @Query(value = "select * from user u "
      + "where u.gender <> :gender and u.id not in :banIds "
      + "order by RAND() limit 100", nativeQuery = true)
  public List<User> findRandomUsers(Gender gender, @Param("banIds") List<Long> banIds);
}
