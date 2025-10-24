package com.lingo.lingoproject.report.dto;

import com.lingo.lingoproject.domain.enums.ReportIntensity;
import com.lingo.lingoproject.domain.enums.ReportStatus;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GetReportInfoResponseDto {
  private Long reportId;
  private Long reportUserId;
  private Long reportedUserId;
  private String intensity;
  private String status;

  @QueryProjection
  public GetReportInfoResponseDto(Long id, Long reportUserId, Long reportedUserId,
      ReportIntensity reportIntensity, ReportStatus reportedUserStatus){
    this.reportId = id;
    this.reportUserId = reportUserId;
    this.reportedUserId = reportedUserId;
    this.intensity = reportIntensity.toString();
    this.status = reportedUserStatus.toString();
  }
}
