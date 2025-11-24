package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.BlockedFriend;

import com.lingo.lingoproject.domain.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BlockedFriendRepository extends JpaRepository<BlockedFriend, Long> {

  /**
   * blackedFriend 에는 유저(나) id와 유저가(내가) 차단한 연락처가 저장되어 있다.
   * blockedFriend와 User를 연락처에 대해서 조인하면
   * 유저의(나의) id와 유저가(내가) 차단한 유저의 id 쌍을 구할 수 있다.
   *
   * 아래 쿼리는
   *  나를 차단한 유저(when u.id = :userId then b.user) 와
   *  내가 차단한 유저(else u)
   * 의 목록을 조회한다.
   */
  @Query("""
    select
        case
           when u.id = :userId then b.user
           else u
        end
    from BlockedFriend b join User u on b.phoneNumber = u.phoneNumber
    where b.user.id = :userId or u.id = :userId
    """)
  List<User> findUsersMutuallyBlockedWith(Long userId);

  void deleteByUser(User user);
}
