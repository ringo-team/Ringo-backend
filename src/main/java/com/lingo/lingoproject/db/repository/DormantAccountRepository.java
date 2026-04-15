package com.lingo.lingoproject.db.repository;

import com.lingo.lingoproject.db.domain.DormantAccount;
import com.lingo.lingoproject.db.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DormantAccountRepository extends JpaRepository<DormantAccount, Long> {

  void deleteByUser(User user);

  boolean existsByUser(User user);
}
