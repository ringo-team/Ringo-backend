package com.lingo.lingoproject.report.domain.service;

import com.lingo.lingoproject.shared.domain.model.ReportStatus;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import com.lingo.lingoproject.shared.infrastructure.persistence.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * 신고 처리에 관한 도메인 규칙을 담당하는 Domain Service.
 *
 * <p>중복 신고 방지, 이용정지 기간 결정, 영구 정지 판별 등
 * 신고 도메인의 비즈니스 불변 조건을 캡슐화한다.</p>
 *
 * <h2>이용정지 기간 규칙</h2>
 * <ul>
 *   <li>MINOR  → {@value #MINOR_SUSPENSION_DAYS}일</li>
 *   <li>RISKY  → {@value #RISKY_SUSPENSION_DAYS}일</li>
 *   <li>SEVERE → {@value #SEVERE_SUSPENSION_DAYS}일</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportDomainService {

  public static final int MINOR_SUSPENSION_DAYS  = 2;
  public static final int RISKY_SUSPENSION_DAYS  = 3;
  public static final int SEVERE_SUSPENSION_DAYS = 7;

  private final ReportRepository reportRepository;

  /**
   * 동일한 신고자·피신고자 쌍으로 이미 PENDING 상태의 신고가 있는지 검증한다.
   *
   * @throws RingoException 중복 신고인 경우
   */
  public void validateNoDuplicateReport(Long reportUserId, Long reportedUserId) {
    if (reportRepository.existsByReportUserIdAndReportedUserIdAndReportedUserStatus(
        reportUserId, reportedUserId, ReportStatus.PENDING)) {
      log.warn("step=신고_중복_접수, reportUserId={}, reportedUserId={}", reportUserId, reportedUserId);
      throw new RingoException("이미 접수된 신고입니다.", ErrorCode.DUPLICATED, HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * 조치 유형에 따른 이용정지 기간(일)을 반환한다.
   *
   * @param status 조치 유형
   * @return 이용정지 기간(일). 기간제 정지가 아닌 경우 0 반환.
   */
  public int determineSuspensionDays(ReportStatus status) {
    return switch (status) {
      case MINOR_ACCOUNT_SUSPENSION  -> MINOR_SUSPENSION_DAYS;
      case RISKY_ACCOUNT_SUSPENSION  -> RISKY_SUSPENSION_DAYS;
      case SEVERE_ACCOUNT_SUSPENSION -> SEVERE_SUSPENSION_DAYS;
      default                        -> 0;
    };
  }

  /**
   * 영구 이용정지 조치인지 여부를 반환한다.
   *
   * @param status 조치 유형
   * @return 영구 정지이면 {@code true}
   */
  public boolean isPermanentSuspension(ReportStatus status) {
    return status == ReportStatus.PERMANENT_ACCOUNT_SUSPENSION;
  }
}