package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.shared.domain.model.FeedImage;
import com.lingo.lingoproject.shared.domain.model.User;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface FeedImageRepository extends JpaRepository<FeedImage, Long> {

  List<FeedImage> findAllByUser(User user);

  void deleteAllByUser(User user);

  int countByUser(User user);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT COUNT(f) FROM FeedImage f WHERE f.user = :user")
  int countByUserWithLock(@Param("user") User user);
}
