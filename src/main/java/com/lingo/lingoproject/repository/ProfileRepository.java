package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.Profile;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.match.dto.GetUserProfileResponseDto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfileRepository extends JpaRepository<Profile, Long> {

  Profile findByUser(User user);

  List<Profile> findAllByUser(User user);

  Profile findByUserAndOrder(User user, int order);

  @Query("select new com.lingo.lingoproject.match.dto.GetUserProfileResponseDto(u.id, u.age, u.gender, u.nickname, p.imageUrl)"
      + "from User u join Profile p on p.user = u "
      + "where u.id in :userIds")
  List<GetUserProfileResponseDto> getUserProfilesByUserIds(@Param("userIds") List<Long> userIds);
}
