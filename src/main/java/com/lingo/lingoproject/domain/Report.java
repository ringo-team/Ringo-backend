package com.lingo.lingoproject.domain;

import com.lingo.lingoproject.domain.enums.ReportIntensity;
import com.lingo.lingoproject.domain.enums.ReportStatus;
import com.lingo.lingoproject.utils.Timestamp;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long reportUserId;

  private Long reportedUserId;

  private String reason;

  private ReportStatus reportedUserStatus;

  private ReportIntensity reportIntensity;

  private Long adminId;

}
