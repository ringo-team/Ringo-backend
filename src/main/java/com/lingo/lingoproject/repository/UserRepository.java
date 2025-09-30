package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByEmail(String email);

  @Query(value = "select * from user u order by RAND() limit 100", nativeQuery = true)
  public List<User> findRandomUsers();
}
