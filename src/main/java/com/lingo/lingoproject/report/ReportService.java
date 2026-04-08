package com.lingo.lingoproject.report;

import com.lingo.lingoproject.domain.Report;
import com.lingo.lingoproject.domain.enums.ReportIntensity;
import com.lingo.lingoproject.domain.enums.ReportStatus;
import com.lingo.lingoproject.exception.ErrorCode;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.report.dto.GetReportInfoRequestDto;
import com.lingo.lingoproject.report.dto.GetReportInfoResponseDto;
import com.lingo.lingoproject.report.dto.SaveReportRequestDto;
import com.lingo.lingoproject.repository.ReportRepository;
import com.lingo.lingoproject.repository.impl.ReportRepositoryImpl;
import com.lingo.lingoproject.user.UserService;
import com.lingo.lingoproject.utils.RedisUtils;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

  private final ReportRepository reportRepository;
  private final ReportRepositoryImpl reportRepositoryImpl;
  private final RedisUtils redisUtils;
  private final UserService userService;

  private final int MINOR_ACCOUNT_SUSPENSION_INTERVAL_DAY = 2;
  private final int RISKY_ACCOUNT_SUSPENSION_INTERVAL_DAY = 3;
  private final int SEVERE_ACCOUNT_SUSPENSION_INTERVAL_DAY = 7;
  private final RedisTemplate<String, Object> redisTemplate;

  public void report(SaveReportRequestDto dto){
    if (reportRepository.existsByReportUserIdAndReportedUserIdAndReportedUserStatus(
        dto.reportUserId(), dto.reportedUserId(), ReportStatus.PENDING
    )){
      log.warn("""

          step=신고_중복_접수,
          reportUserId={},
          reportedUserId={},
          status=DUPLICATED

          """, dto.reportUserId(), dto.reportedUserId());
      throw new RingoException("이미 접수된 신고입니다.", ErrorCode.DUPLICATED, HttpStatus.BAD_REQUEST);
    }
    Report report = Report.builder()
        .reportUserId(dto.reportUserId())
        .reportedUserId(dto.reportedUserId())
        .reason(dto.reason())
        .reportedUserStatus(ReportStatus.PENDING)
        .reportIntensity(ReportIntensity.PENDING)
        .build();
    reportRepository.save(report);
    log.info("""

        step=신고_접수_완료,
        reportUserId={},
        reportedUserId={}

        """, dto.reportUserId(), dto.reportedUserId());
  }

  public List<GetReportInfoResponseDto> getReportInfos(GetReportInfoRequestDto dto){
    return reportRepositoryImpl.findReportInfo(dto);
  }

  public void suspendUser(Long reportId, String reportedUserStatus, Long adminId){
    log.info("""

        step=신고_처리_시작,
        reportId={},
        reportedUserStatus={},
        adminId={}

        """, reportId, reportedUserStatus, adminId);
    Report report = reportRepository.findById(reportId)
        .orElseThrow(() -> new RingoException("해당 신고가 존재하지 않습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));
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
        redisTemplate.opsForValue().set("suspension::" + report.getReportedUserId(), true, MINOR_ACCOUNT_SUSPENSION_INTERVAL_DAY, TimeUnit.DAYS);
        break;
      case "RISKY_ACCOUNT_SUSPENSION":
        report.setReportedUserStatus(ReportStatus.RISKY_ACCOUNT_SUSPENSION);
        redisTemplate.opsForValue().set("suspension::" + report.getReportedUserId(), true, RISKY_ACCOUNT_SUSPENSION_INTERVAL_DAY, TimeUnit.DAYS);
        break;
      case "SEVERE_ACCOUNT_SUSPENSION":
        report.setReportedUserStatus(ReportStatus.SEVERE_ACCOUNT_SUSPENSION);
        redisTemplate.opsForValue().set("suspension::" + report.getReportedUserId(), true, SEVERE_ACCOUNT_SUSPENSION_INTERVAL_DAY, TimeUnit.DAYS);
        break;
      case "PERMANENT_ACCOUNT_SUSPENSION":
        report.setReportedUserStatus(ReportStatus.PERMANENT_ACCOUNT_SUSPENSION);
        userService.blockUser(report.getReportedUserId(), adminId);
      case "LEGAL_REVIEW":
        report.setReportedUserStatus(ReportStatus.LEGAL_REVIEW);
        break;
      default:
        log.warn("""

            step=신고_처리_잘못된_조치,
            reportId={},
            reportedUserStatus={},
            adminId={}

            """, reportId, reportedUserStatus, adminId);
        throw new RingoException("적절하지 못한 조치가 입력되었습니다.", ErrorCode.BAD_PARAMETER, HttpStatus.BAD_REQUEST);
    }
    log.info("""

        step=신고_처리_완료,
        reportId={},
        reportedUserId={},
        reportedUserStatus={},
        adminId={}

        """, reportId, report.getReportedUserId(), reportedUserStatus, adminId);
    reportRepository.save(report);
  }
}
