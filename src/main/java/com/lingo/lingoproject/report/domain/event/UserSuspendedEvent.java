package com.lingo.lingoproject.report.domain.event;

import com.lingo.lingoproject.shared.domain.event.DomainEvent;
import com.lingo.lingoproject.shared.domain.event.DomainEventType;

public class UserSuspendedEvent extends DomainEvent {
  private final Long reportedUserId;
  private final Long adminId;
  private final boolean permanent;

  public UserSuspendedEvent(Long reportedUserId, Long adminId, boolean permanent) {
    super(DomainEventType.USER_SUSPENDED);
    this.reportedUserId = reportedUserId;
    this.adminId        = adminId;
    this.permanent      = permanent;
  }

  public Long    getReportedUserId() { return reportedUserId; }
  public Long    getAdminId()        { return adminId; }
  public boolean isPermanent()       { return permanent; }
}
