package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.Report;
import com.lingo.lingoproject.domain.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

  boolean existsByReportUserIdAndReportedUserIdAndReportedUserStatus(Long reportUserId, Long reportedUserId, ReportStatus reportedUserStatus);
}
