package com.lingo.lingoproject.notification.application;

import com.lingo.lingoproject.matching.domain.event.MatchingAcceptedEvent;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.user.application.UserQueryUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 매칭 수락 이벤트를 수신하여 매칭을 요청한 유저에게 FCM 알림을 전송한다.
 * MatchingAcceptedEventHandler(채팅방 생성)와 별도로 notification 컨텍스트에서 처리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MatchingAcceptedNotificationHandler {

  private final FcmNotificationUseCase fcmNotificationUseCase;
  private final UserQueryUseCase userQueryUseCase;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(MatchingAcceptedEvent event) {
    log.info("MatchingAcceptedEvent(알림) 수신: matchingId={}, requestUser={}",
        event.getMatchingId(), event.getRequestUserId());
    User requestUser = userQueryUseCase.findUserOrThrow(event.getRequestUserId());
    //fcmNotificationUseCase.sendFcmNotification(requestUser, "누군가 요청을 수락했어요", null, NotificationType.MATCHING_ACCEPTED);
  }
}
