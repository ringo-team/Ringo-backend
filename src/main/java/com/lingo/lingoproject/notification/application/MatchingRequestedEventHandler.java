package com.lingo.lingoproject.notification.application;

import com.lingo.lingoproject.matching.domain.event.MatchingRequestedEvent;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.user.application.UserQueryUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 매칭 요청 이벤트를 수신하여 요청 받은 유저에게 FCM 알림을 전송한다.
 * MatchService가 FcmNotificationUseCase를 직접 호출하던 강결합을 이벤트로 해소한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MatchingRequestedEventHandler {

  private final FcmNotificationUseCase fcmNotificationUseCase;
  private final UserQueryUseCase userQueryUseCase;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(MatchingRequestedEvent event) {
    log.info("MatchingRequestedEvent 수신: matchingId={}, requestUser={}, requestedUser={}",
        event.getMatchingId(), event.getRequestUserId(), event.getRequestedUserId());
    User requestedUser = userQueryUseCase.findUserOrThrow(event.getRequestedUserId());
    //fcmNotificationUseCase.sendFcmNotification(requestedUser, "누군가 매칭 요청을 했어요!", null, NotificationType.MATCHING_REQUEST);
  }
}
