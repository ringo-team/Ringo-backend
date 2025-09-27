package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.Profile;
import com.lingo.lingoproject.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepository extends JpaRepository<Profile, Long> {

  Profile findByUser(User user);

  Profile findByUserAndOrder(User user, int order);
}
