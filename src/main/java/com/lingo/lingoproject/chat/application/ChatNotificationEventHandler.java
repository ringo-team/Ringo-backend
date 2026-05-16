package com.lingo.lingoproject.chat.application;

import com.lingo.lingoproject.notification.application.FcmNotificationUseCase;
import com.lingo.lingoproject.shared.domain.model.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ChatNotificationEventHandler {
  private final FcmNotificationUseCase fcmNotificationUseCase;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(ChatNotificationEvent event){
    fcmNotificationUseCase.sendFcmNotification(
        event.getMember(),
        event.getSender().getProfile().getImageUrl(),
        event.getSender().getNickname(),
        event.getMessage(),
        NotificationType.MESSAGE
    );
  }
}
