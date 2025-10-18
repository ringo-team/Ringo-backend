package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.DormantAccount;
import com.lingo.lingoproject.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DormantAccountRepository extends JpaRepository<DormantAccount, Long> {

  void deleteByUser(User user);
}
