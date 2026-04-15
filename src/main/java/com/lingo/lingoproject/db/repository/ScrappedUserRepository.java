package com.lingo.lingoproject.db.repository;

import com.lingo.lingoproject.db.domain.ScrappedUser;
import com.lingo.lingoproject.db.domain.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScrappedUserRepository extends JpaRepository<ScrappedUser, Long> {

  List<ScrappedUser> findByUser(User user);

  List<ScrappedUser> findAllByUser(User user);
}
