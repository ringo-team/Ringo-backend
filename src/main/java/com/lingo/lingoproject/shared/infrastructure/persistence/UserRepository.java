package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.shared.domain.model.SignupStatus;
import com.lingo.lingoproject.shared.domain.model.User;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByLoginId(String loginId);

  Optional<User> findByFriendInvitationCode(String friendInvitationCode);

  List<User> findAllByIdIn(Collection<Long> ids);

  boolean existsByLoginId(String loginId);

  List<User> findAllByCreatedAtAfter(LocalDateTime createdAtAfter);

  boolean existsByNickname(String nickname);

  Optional<User> findByPhoneNumber(String phoneNumber);

  Optional<User> findByLoginIdAndPhoneNumber(String loginId, String phoneNumber);

  @Query("select u.id from User u where u.id not in :excludedUserIds")
  List<Long> findByUserIdNotInExcludedUserIds(List<Long> excludedUserIds);

  List<User> findAllByStatusNot(SignupStatus status);
}
