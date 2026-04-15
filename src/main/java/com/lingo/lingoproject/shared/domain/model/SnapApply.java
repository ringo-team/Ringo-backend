package com.lingo.lingoproject.shared.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "SNAP_APPLIES")
public class SnapApply {

  public static SnapApply of(User photographer, Long userId, LocalDateTime snapStartedAt,
      Integer duration, String snapLocation, String meetingLocation) {
    return SnapApply.builder()
        .photographer(photographer)
        .userId(userId)
        .snapStartedAt(snapStartedAt)
        .duration(duration)
        .snapLocation(snapLocation)
        .meetingLocation(meetingLocation)
        .build();
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "photographer_id")
  private User photographer;

  private Long userId;

  private LocalDateTime snapStartedAt;
  private Integer duration;


  private String snapLocation;
  private String meetingLocation;
  private String keyword;
  private String extraRequest;

  @Builder.Default
  private ApprovalType approvalStatus = ApprovalType.REQUESTED;
}
