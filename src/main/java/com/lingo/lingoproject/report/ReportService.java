package com.lingo.lingoproject.report;

import com.lingo.lingoproject.domain.Report;
import com.lingo.lingoproject.domain.enums.ReportStatus;
import com.lingo.lingoproject.report.dto.SaveReportRequestDto;
import com.lingo.lingoproject.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportService {

  private final ReportRepository reportRepository;

  public void report(SaveReportRequestDto dto){
    Report report = Report.builder()
        .reportUserId(dto.reportUserId())
        .reportedUserId(dto.reportedUserId())
        .reason(dto.reason())
        .status(ReportStatus.PENDING)
        .build();
    reportRepository.save(report);
  }
}
