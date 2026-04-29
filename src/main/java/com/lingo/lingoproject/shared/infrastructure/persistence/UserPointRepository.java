package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.domain.model.UserPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface UserPointRepository extends JpaRepository<UserPoint, Long> {

  void deleteAllByUser(User user);

  @Modifying
  @Query("update UserPoint p set p.point = p.point + :point where p.user = :user")
  void updateUserPoint(int point, User user);

  UserPoint findByUser(User user);
}
