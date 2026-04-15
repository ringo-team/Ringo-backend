package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.shared.domain.model.Withdrawer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WithdrawerRepository extends JpaRepository<Withdrawer, Long> {

}
