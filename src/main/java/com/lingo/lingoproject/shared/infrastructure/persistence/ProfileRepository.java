package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.shared.domain.model.Profile;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.matching.presentation.dto.GetUserProfileResponseDto;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfileRepository extends JpaRepository<Profile, Long> {

  Optional<Profile> findByUser(User user);

  @Query("select p.user from Profile p where p.user.id not in :excludedUserIds order by p.clickCount desc limit 5")
  List<User> findUsersOrderByProfileClickCountLimitFive(List<Long> excludedUserIds);

  @Query("select p from Profile p join User u where u.id in :list")
  List<Profile> findProfileByUserIdIn(@Param("list") Set<Long> list);

  @Query("""
      select
        new com.lingo.lingoproject.matching.presentation.dto.GetUserProfileResponseDto(
          u.id,
          u.gender,
          u.nickname,
          p.imageUrl,
          m.matchingScore,
          m.id,
          m.matchingStatus,
          m.matchingRequestMessage,
          p.faceVerify
      )
      from Matching m
        join m.requestedUser u
        join u.profile p
      where m.id in :matchingIds
  """)
  List<GetUserProfileResponseDto> getRequestedUserProfilesByMatchingIds(List<Long> matchingIds);

  @Query("""
      select
        new com.lingo.lingoproject.matching.presentation.dto.GetUserProfileResponseDto(
          u.id,
          u.gender,
          u.nickname,
          p.imageUrl,
          m.matchingScore,
          m.id,
          m.matchingStatus,
          m.matchingRequestMessage,
          p.faceVerify
      )
      from Matching m
        join m.requestUser u
        join u.profile p
      where m.id in :matchingIds
  """)
  List<GetUserProfileResponseDto> getRequestUserProfilesByMatchingIds(List<Long> matchingIds);

  boolean existsByUser(User user);

  @Modifying
  @Query("update Profile p set p.clickCount = p.clickCount + 1 where p.id = :profileId")
  void updateProfileClickCount(Long profileId);
}
