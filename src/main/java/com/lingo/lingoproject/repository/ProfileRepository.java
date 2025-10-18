package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.Matching;
import com.lingo.lingoproject.domain.Profile;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.match.dto.GetUserProfileResponseDto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfileRepository extends JpaRepository<Profile, Long> {

  Optional<Profile> findByUser(User user);


  @Query("select new com.lingo.lingoproject.match.dto.GetUserProfileResponseDto"
      + "(u.id, u.age, u.gender, u.nickname, p.imageUrl, m.matchingScore) "
      + "from Matching m join User u on m.requestedUser = u "
      + "join Profile p on m.requestedUser = p.user where m.id in :matchingIds")
  List<GetUserProfileResponseDto> getRequestedUserProfilesByUserIds(List<Long> matchingIds);

  @Query("select new com.lingo.lingoproject.match.dto.GetUserProfileResponseDto"
      + "(u.id, u.age, u.gender, u.nickname, p.imageUrl, m.matchingScore) "
      + "from Matching m join User u on m.requestUser = u "
      + "join Profile p on m.requestUser = p.user where m.id in :matchingIds")
  List<GetUserProfileResponseDto> getRequestUserProfilesByUserIds(List<Long> matchingIds);

  List<Profile> findAllByUser(User user);

  void deleteAllByUser(User user);
}
