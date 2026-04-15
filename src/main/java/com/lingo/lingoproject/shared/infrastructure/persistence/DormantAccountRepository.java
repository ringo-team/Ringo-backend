package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.shared.domain.model.DormantAccount;
import com.lingo.lingoproject.shared.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DormantAccountRepository extends JpaRepository<DormantAccount, Long> {

  void deleteByUser(User user);

  boolean existsByUser(User user);
}
