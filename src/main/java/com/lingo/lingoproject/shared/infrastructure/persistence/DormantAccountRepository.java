package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.shared.domain.model.DormantAccount;
import com.lingo.lingoproject.shared.domain.model.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DormantAccountRepository extends JpaRepository<DormantAccount, Long> {

  void deleteByUser(User user);

  boolean existsByUser(User user);

  @Query("SELECT d.user.id FROM DormantAccount d")
  List<Long> findAllDormantUserIds();
}
