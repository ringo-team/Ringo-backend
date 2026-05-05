package com.lingo.lingoproject.report.application;

import com.lingo.lingoproject.report.domain.event.UserSuspendedEvent;
import com.lingo.lingoproject.report.domain.service.ReportDomainService;
import com.lingo.lingoproject.report.presentation.dto.GetReportInfoRequestDto;
import com.lingo.lingoproject.report.presentation.dto.GetReportInfoResponseDto;
import com.lingo.lingoproject.report.presentation.dto.SaveReportRequestDto;
import com.lingo.lingoproject.shared.domain.event.DomainEventPublisher;
import com.lingo.lingoproject.shared.domain.model.Report;
import com.lingo.lingoproject.shared.domain.model.ReportStatus;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import com.lingo.lingoproject.shared.infrastructure.persistence.ReportRepository;
import com.lingo.lingoproject.shared.utils.RedisUtils;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 신고 접수 및 관리자 조치 처리 비즈니스 로직을 담당하는 서비스.
 *
 * <h2>신고 상태 흐름</h2>
 * <pre>
 *   PENDING → INNOCENT_REPORT | WARNING
 *           → MINOR_ACCOUNT_SUSPENSION (2일)
 *           → RISKY_ACCOUNT_SUSPENSION  (3일)
 *           → SEVERE_ACCOUNT_SUSPENSION (7일)
 *           → PERMANENT_ACCOUNT_SUSPENSION
 *           → LEGAL_REVIEW
 * </pre>
 *
 * <h2>이용정지 처리</h2>
 * <p>기간제 이용정지({@code MINOR}/{@code RISKY}/{@code SEVERE})는 Redis
 * {@code suspension::{userId}} 키에 해당 기간(TTL)을 설정합니다.
 * 영구 정지({@code PERMANENT_ACCOUNT_SUSPENSION})는 {@link UserSuspendedEvent}를 발행하여
 * 계정 비활성화 등의 후속 처리를 트리거합니다.</p>
 *
 * <h2>중복 신고 방지</h2>
 * <p>동일 신고자가 동일 피신고자에 대해 {@code PENDING} 상태의 신고가 이미 있으면
 * {@code 400 BAD_REQUEST}를 반환합니다.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

  private final ReportRepository reportRepository;
  private final RedisUtils redisUtils;
  private final DomainEventPublisher eventPublisher;
  private final ReportDomainService reportDomainService;
  private final RedisTemplate<String, Object> redisTemplate;

  /**
   * 신고를 접수한다.
   *
   * <p>동일 신고자·피신고자 쌍으로 이미 {@code PENDING} 상태의 신고가 있으면
   * 중복 신고로 간주하여 예외를 발생시킵니다.</p>
   *
   * @param dto 신고자 ID, 피신고자 ID, 신고 사유가 담긴 DTO
   * @throws RingoException 이미 처리 대기 중인 신고가 존재하는 경우
   */
  public void report(SaveReportRequestDto dto){
    reportDomainService.validateNoDuplicateReport(dto.reportUserId(), dto.reportedUserId());
    reportRepository.save(Report.of(dto.reportUserId(), dto.reportedUserId(), dto.reason()));
    log.info("step=신고_접수_완료, reportUserId={}, reportedUserId={}", dto.reportUserId(), dto.reportedUserId());
  }

  /**
   * 신고 목록을 조회한다.
   *
   * <p>관리자 화면에서 신고 현황 확인에 사용됩니다.
   * 필터 조건({@link GetReportInfoRequestDto})을 기반으로 신고 내역을 반환합니다.</p>
   *
   * @param dto 조회 필터 DTO (신고 상태, 날짜 범위 등)
   * @return 신고 정보 목록
   */
  public List<GetReportInfoResponseDto> getReportInfos(GetReportInfoRequestDto dto){
    return reportRepository.findReportInfo(dto);
  }

  /**
   * 신고에 대한 관리자 조치를 처리한다.
   *
   * <p>조치 유형에 따라 신고 상태를 업데이트하고,
   * 기간제 이용정지는 Redis에 TTL을 설정하며,
   * 영구 정지는 {@link UserSuspendedEvent}를 발행하여 계정을 비활성화합니다.</p>
   *
   * <p><b>조치 유형 목록</b></p>
   * <ul>
   *   <li>{@code INNOCENT_REPORT} — 무혐의 처리</li>
   *   <li>{@code WARNING} — 경고</li>
   *   <li>{@code MINOR_ACCOUNT_SUSPENSION} — 2일 이용정지</li>
   *   <li>{@code RISKY_ACCOUNT_SUSPENSION} — 3일 이용정지</li>
   *   <li>{@code SEVERE_ACCOUNT_SUSPENSION} — 7일 이용정지</li>
   *   <li>{@code PERMANENT_ACCOUNT_SUSPENSION} — 영구 정지 (이벤트 발행)</li>
   *   <li>{@code LEGAL_REVIEW} — 법적 검토</li>
   * </ul>
   *
   * @param reportId           처리할 신고 ID
   * @param reportedUserStatus 조치 유형 문자열
   * @param adminId            처리를 수행한 관리자 ID
   * @throws RingoException 신고가 존재하지 않거나 알 수 없는 조치 유형인 경우
   */
  @Transactional
  public void suspendUser(Long reportId, String reportedUserStatus, Long adminId){
    log.info("step=신고_처리_시작, reportId={}, reportedUserStatus={}, adminId={}", reportId, reportedUserStatus, adminId);
    Report report = reportRepository.findById(reportId)
        .orElseThrow(() -> new RingoException("해당 신고가 존재하지 않습니다.", ErrorCode.NOT_FOUND));
    report.setAdminId(adminId);

    ReportStatus status = parseReportStatus(reportedUserStatus, reportId, adminId);
    report.setReportedUserStatus(status);

    int suspensionDays = reportDomainService.determineSuspensionDays(status);
    if (suspensionDays > 0) {
      redisTemplate.opsForValue().set(
          "suspension::" + report.getReportedUserId(), true, suspensionDays, TimeUnit.DAYS);
    }

    if (reportDomainService.isPermanentSuspension(status)) {
      eventPublisher.publish(new UserSuspendedEvent(report.getReportedUserId(), adminId, true));
    }
    log.info("step=신고_처리_완료, reportId={}, reportedUserId={}, reportedUserStatus={}, adminId={}", reportId, report.getReportedUserId(), reportedUserStatus, adminId);
    reportRepository.save(report);
  }

  private ReportStatus parseReportStatus(String reportedUserStatus, Long reportId, Long adminId) {
    return switch (reportedUserStatus) {
      case "INNOCENT_REPORT"           -> ReportStatus.INNOCENT_REPORT;
      case "WARNING"                   -> ReportStatus.WARNING;
      case "MINOR_ACCOUNT_SUSPENSION"  -> ReportStatus.MINOR_ACCOUNT_SUSPENSION;
      case "RISKY_ACCOUNT_SUSPENSION"  -> ReportStatus.RISKY_ACCOUNT_SUSPENSION;
      case "SEVERE_ACCOUNT_SUSPENSION" -> ReportStatus.SEVERE_ACCOUNT_SUSPENSION;
      case "PERMANENT_ACCOUNT_SUSPENSION" -> ReportStatus.PERMANENT_ACCOUNT_SUSPENSION;
      case "LEGAL_REVIEW"              -> ReportStatus.LEGAL_REVIEW;
      default -> {
        log.warn("step=신고_처리_잘못된_조치, reportId={}, reportedUserStatus={}, adminId={}",
            reportId, reportedUserStatus, adminId);
        throw new RingoException("적절하지 못한 조치가 입력되었습니다.", ErrorCode.BAD_PARAMETER);
      }
    };
  }
}
