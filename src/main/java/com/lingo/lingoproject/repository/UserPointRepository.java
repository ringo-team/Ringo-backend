package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.domain.UserPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface UserPointRepository extends JpaRepository<UserPoint, Long> {

  void deleteAllByUser(User user);

  @Modifying
  @Query("update UserPoint p set p.point = p.point + :point where p.user = :user")
  void updateUserPoint(int point, User user);

}
