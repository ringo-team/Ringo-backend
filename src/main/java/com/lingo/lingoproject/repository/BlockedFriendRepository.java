package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.BlockedFriend;

import com.lingo.lingoproject.domain.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BlockedFriendRepository extends JpaRepository<BlockedFriend, Long> {
  @Query("select b.user from BlockedFriend b join User u on b.phoneNumber=u.phoneNumber and  u.id = :userId"
      + " union all "
      + "select u from BlockedFriend b join User u on b.phoneNumber=u.phoneNumber and b.id = :userId")
  List<User> findBlockedFriends(Long userId); // 나를 block한 사람과 내가 block한 사람
}
