package com.lingo.lingoproject.user.application;

import com.lingo.lingoproject.report.domain.event.UserSuspendedEvent;
import com.lingo.lingoproject.shared.domain.model.BlockedUser;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.infrastructure.persistence.BlockedUserRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.UserRepository;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 유저 정지 이벤트를 수신하여 영구정지인 경우 BlockedUser를 저장한다.
 * ReportService가 UserService를 직접 호출하던 강결합을 이벤트로 해소한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserSuspensionEventHandler {

  private final UserRepository userRepository;
  private final BlockedUserRepository blockedUserRepository;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(UserSuspendedEvent event) {
    if (!event.isPermanent()) return;

    log.info("UserSuspendedEvent(영구정지) 수신: reportedUserId={}", event.getReportedUserId());

    User admin = userRepository.findById(event.getAdminId())
        .orElseThrow(() -> new RingoException("관리자를 찾을 수 없습니다.", ErrorCode.NOT_FOUND_ADMIN, HttpStatus.NOT_FOUND));
    User user = userRepository.findById(event.getReportedUserId())
        .orElseThrow(() -> new RingoException("유저를 찾을 수 없습니다.", ErrorCode.NOT_FOUND_USER, HttpStatus.NOT_FOUND));

    blockedUserRepository.save(BlockedUser.of(user, admin));
    log.info("BlockedUser 저장 완료: userId={}", user.getId());
  }
}
