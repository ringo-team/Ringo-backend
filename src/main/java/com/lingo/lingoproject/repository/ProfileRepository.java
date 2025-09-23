package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.Profile;
import com.lingo.lingoproject.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepository extends JpaRepository<Profile, Long> {

  Profile findByUser(UserEntity user);

  Profile findByUserAndOrder(UserEntity user, int order);
}
