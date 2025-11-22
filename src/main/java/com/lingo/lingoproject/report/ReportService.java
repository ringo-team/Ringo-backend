package com.lingo.lingoproject.report;

import com.lingo.lingoproject.domain.Report;
import com.lingo.lingoproject.domain.enums.ReportIntensity;
import com.lingo.lingoproject.domain.enums.ReportStatus;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.report.dto.GetReportInfoRequestDto;
import com.lingo.lingoproject.report.dto.GetReportInfoResponseDto;
import com.lingo.lingoproject.report.dto.SaveReportRequestDto;
import com.lingo.lingoproject.repository.ReportRepository;
import com.lingo.lingoproject.repository.impl.ReportRepositoryImpl;
import com.lingo.lingoproject.user.UserService;
import com.lingo.lingoproject.utils.RedisUtils;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportService {

  private final ReportRepository reportRepository;
  private final ReportRepositoryImpl reportRepositoryImpl;
  private final RedisUtils redisUtils;
  private final UserService userService;

  private final int MINOR_ACCOUNT_SUSPENSION_INTERVAL_DAY = 2;
  private final int RISKY_ACCOUNT_SUSPENSION_INTERVAL_DAY = 3;
  private final int SEVERE_ACCOUNT_SUSPENSION_INTERVAL_DAY = 7;

  public void report(SaveReportRequestDto dto){
    if (reportRepository.existsByReportUserIdAndReportedUserIdAndReportedUserStatus(
        dto.reportUserId(), dto.reportedUserId(), ReportStatus.PENDING
    )){
      throw new RingoException("이미 접수된 신고입니다.", HttpStatus.BAD_REQUEST);
    }
    Report report = Report.builder()
        .reportUserId(dto.reportUserId())
        .reportedUserId(dto.reportedUserId())
        .reason(dto.reason())
        .reportedUserStatus(ReportStatus.PENDING)
        .reportIntensity(ReportIntensity.PENDING)
        .build();
    reportRepository.save(report);
  }

  public List<GetReportInfoResponseDto> getReportInfos(GetReportInfoRequestDto dto){
    return reportRepositoryImpl.findReportInfo(dto);
  }

  public void suspendUser(Long reportId, String reportedUserStatus, Long adminId){
    Report report = reportRepository.findById(reportId)
        .orElseThrow(() -> new RingoException("해당 신고가 존재하지 않습니다.", HttpStatus.BAD_REQUEST));
    report.setAdminId(adminId);
    switch (reportedUserStatus){
      case "INNOCENT_REPORT":
        report.setReportedUserStatus(ReportStatus.INNOCENT_REPORT);
        break;
      case "WARNING":
        report.setReportedUserStatus(ReportStatus.WARNING);
        break;
      case "MINOR_ACCOUNT_SUSPENSION":
        report.setReportedUserStatus(ReportStatus.MINOR_ACCOUNT_SUSPENSION);
        redisUtils.suspendUser(report.getReportedUserId(), MINOR_ACCOUNT_SUSPENSION_INTERVAL_DAY);
        break;
      case "RISKY_ACCOUNT_SUSPENSION":
        report.setReportedUserStatus(ReportStatus.RISKY_ACCOUNT_SUSPENSION);
        redisUtils.suspendUser(report.getReportedUserId(), RISKY_ACCOUNT_SUSPENSION_INTERVAL_DAY);
        break;
      case "SEVERE_ACCOUNT_SUSPENSION":
        report.setReportedUserStatus(ReportStatus.SEVERE_ACCOUNT_SUSPENSION);
        redisUtils.suspendUser(report.getReportedUserId(), SEVERE_ACCOUNT_SUSPENSION_INTERVAL_DAY);
        break;
      case "PERMANENT_ACCOUNT_SUSPENSION":
        report.setReportedUserStatus(ReportStatus.PERMANENT_ACCOUNT_SUSPENSION);
        userService.blockUser(report.getReportedUserId(), adminId);
      case "LEGAL_REVIEW":
        report.setReportedUserStatus(ReportStatus.LEGAL_REVIEW);
        break;
      default:
        throw new RingoException("적절하지 못한 조치가 입력되었습니다.", HttpStatus.BAD_REQUEST);
    }
    reportRepository.save(report);
  }
}
