package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.shared.domain.model.Profile;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.matching.presentation.dto.GetUserProfileResponseDto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProfileRepository extends JpaRepository<Profile, Long> {

  Optional<Profile> findByUser(User user);


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
          p.inspectStatus
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
          p.inspectStatus
      )
      from Matching m
        join m.requestUser u
        join u.profile p
      where m.id in :matchingIds
  """)
  List<GetUserProfileResponseDto> getRequestUserProfilesByMatchingIds(List<Long> matchingIds);

  boolean existsByUser(User user);
}
