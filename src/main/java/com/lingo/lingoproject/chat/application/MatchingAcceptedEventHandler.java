package com.lingo.lingoproject.chat.application;

import com.lingo.lingoproject.chat.presentation.dto.CreateChatroomRequestDto;
import com.lingo.lingoproject.matching.domain.event.MatchingAcceptedEvent;
import com.lingo.lingoproject.notification.application.FcmNotificationUseCase;
import com.lingo.lingoproject.shared.domain.model.NotificationType;
import com.lingo.lingoproject.shared.domain.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 매칭 수락 이벤트를 수신하여 채팅방을 생성한다.
 * MatchingAcceptedEvent는 matching 컨텍스트에서 발행되고
 * chat 컨텍스트에서 처리함으로써 두 컨텍스트 간의 직접 의존을 제거한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MatchingAcceptedEventHandler {

  private final ChatService chatService;
  private final FcmNotificationUseCase fcmNotificationUseCase;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(MatchingAcceptedEvent event) {
    log.info("MatchingAcceptedEvent 수신: matchingId={}, requestUser={}, requestedUser={}",
        event.getMatchingId(), event.getRequestUserId(), event.getRequestedUserId());
    CreateChatroomRequestDto dto = new CreateChatroomRequestDto(
        event.getRequestUserId(),
        event.getRequestedUserId(),
        "USER"
    );
    chatService.createChatroom(dto);
    User requestedUser = chatService.findUserOrThrow(event.getRequestedUserId());
    /*
    fcmNotificationUseCase.sendFcmNotification(
        requestedUser,
        "매칭이 승낙되었습니다.",
        null,
        NotificationType.MATCHING_ACCEPTED
        );
     */
  }
}
