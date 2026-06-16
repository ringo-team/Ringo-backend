package com.lingo.lingoproject.matching.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingo.lingoproject.matching.domain.event.UserScrapEvent;
import com.lingo.lingoproject.notification.application.FcmNotificationUseCase;
import com.lingo.lingoproject.shared.domain.model.NotificationType;
import com.lingo.lingoproject.shared.domain.model.Profile;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.infrastructure.persistence.UserScrapLogRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UserScrapEventHandler {

  private final FcmNotificationUseCase fcmService;
  private final UserScrapLogRepository userScrapLogRepository;
  private final ObjectMapper objectMapper;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Async
  public void handle(UserScrapEvent event) throws Exception{
    User user = event.getUser();
    User scrappedUser = event.getScrappedUser();

    // 이미 스크랩한 이력이 있으면 알림 전송하지 않는다.
    if (userScrapLogRepository.existsByUserAndScrappedUser(user, scrappedUser)) return;

    Profile senderProfile = user.getProfile();
    String senderImageUrl = senderProfile != null ? senderProfile.getImageUrl() : null;

    String params = objectMapper.writeValueAsString(Map.of("userId", String.valueOf(user.getId())));

    fcmService.sendFcmNotification(
        scrappedUser,
        senderImageUrl,
        event.getUser().getNickname() + "(이)가 회원님을 스크랩했습니다",
        null,
        NotificationType.USER_SCRAP,
        "/scrap/user",
        params
    );
  }
}
