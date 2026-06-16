package com.lingo.lingoproject.chat.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingo.lingoproject.notification.application.FcmNotificationUseCase;
import com.lingo.lingoproject.shared.domain.model.NotificationType;
import com.lingo.lingoproject.shared.domain.model.Profile;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ChatNotificationEventHandler {
  private final FcmNotificationUseCase fcmNotificationUseCase;
  private final ObjectMapper objectMapper;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Async
  public void handle(ChatNotificationEvent event) throws Exception{
    Profile senderProfile = event.getSender().getProfile();
    String params = objectMapper.writeValueAsString(Map.of("roomId", String.valueOf(event.getRoomId())));
    fcmNotificationUseCase.sendFcmNotification(
        event.getMember(),
        senderProfile != null ? senderProfile.getImageUrl() : null,
        event.getSender().getNickname(),
        event.getMessage(),
        NotificationType.MESSAGE,
        "/(tabs)/chat/room",
        params
    );
  }
}
