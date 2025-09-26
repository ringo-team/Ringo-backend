package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.UserEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
  Optional<UserEntity> findByEmail(String email);

  @Query(value = "select * from UserEntity u order by RAND() limit 100", nativeQuery = true)
  public List<UserEntity> findRandomUsers();
}
