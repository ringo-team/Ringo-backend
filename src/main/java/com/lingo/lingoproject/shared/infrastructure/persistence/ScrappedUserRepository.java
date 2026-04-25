package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.shared.domain.model.ScrappedUser;
import com.lingo.lingoproject.shared.domain.model.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScrappedUserRepository extends JpaRepository<ScrappedUser, Long> {

  List<ScrappedUser> findByUser(User user);

  List<ScrappedUser> findAllByUser(User user);

  boolean existsByUserAndScrappedUser(User user, User scrappedUser);

  void deleteByUserAndScrappedUser(User user, User scrappedUser);
}
