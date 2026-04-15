package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.shared.domain.model.SnapApply;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SnapApplyRepository extends JpaRepository<SnapApply, Long> {

}
