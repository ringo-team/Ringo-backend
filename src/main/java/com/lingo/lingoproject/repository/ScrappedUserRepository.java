package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.ScrappedUser;
import com.lingo.lingoproject.domain.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScrappedUserRepository extends JpaRepository<ScrappedUser, Long> {

  List<ScrappedUser> findByUser(User user);

  List<ScrappedUser> findAllByUser(User user);
}
