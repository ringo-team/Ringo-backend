package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.shared.domain.model.MemberShipLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberShipLogRepository extends JpaRepository<MemberShipLog, Long> {
}
