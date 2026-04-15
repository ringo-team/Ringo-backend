package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.shared.domain.model.Report;
import com.lingo.lingoproject.shared.domain.model.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long>, ReportRepositoryCustom {

  boolean existsByReportUserIdAndReportedUserIdAndReportedUserStatus(Long reportUserId, Long reportedUserId, ReportStatus reportedUserStatus);
}
