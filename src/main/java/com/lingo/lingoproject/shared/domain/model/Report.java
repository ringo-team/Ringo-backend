package com.lingo.lingoproject.shared.domain.model;

import com.lingo.lingoproject.shared.domain.model.ReportIntensity;
import com.lingo.lingoproject.shared.domain.model.ReportStatus;
import com.lingo.lingoproject.shared.utils.Timestamp;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "REPORTS")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class Report extends Timestamp {

  public static Report of(Long reportUserId, Long reportedUserId, String reason) {
    return Report.builder()
        .reportUserId(reportUserId)
        .reportedUserId(reportedUserId)
        .reason(reason)
        .reportedUserStatus(ReportStatus.PENDING)
        .reportIntensity(ReportIntensity.PENDING)
        .build();
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long reportUserId;

  private Long reportedUserId;

  @Lob
  private String reason;

  private ReportStatus reportedUserStatus;

  private ReportIntensity reportIntensity;

  private Long adminId;

}
