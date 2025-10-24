package com.lingo.lingoproject.domain.enums;

public enum ReportStatus {
  PENDING, // 검수 전
  INNOCENT_REPORT, // 무죄
  WARNING,  // 경고
  MINOR_ACCOUNT_SUSPENSION, // 48(2일) 계정정지
  RISKY_ACCOUNT_SUSPENSION,   // 72시간(3일) 계정 정지
  SEVERE_ACCOUNT_SUSPENSION, // 168시간(7일) 계정 정지
  PERMANENT_ACCOUNT_SUSPENSION,  // 계정 삭제 및 block
  LEGAL_REVIEW,   // 법적 검토
}
