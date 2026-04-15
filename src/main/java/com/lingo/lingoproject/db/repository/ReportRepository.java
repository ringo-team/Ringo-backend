package com.lingo.lingoproject.db.repository;

import com.lingo.lingoproject.db.domain.Report;
import com.lingo.lingoproject.db.domain.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

  boolean existsByReportUserIdAndReportedUserIdAndReportedUserStatus(Long reportUserId, Long reportedUserId, ReportStatus reportedUserStatus);
}
