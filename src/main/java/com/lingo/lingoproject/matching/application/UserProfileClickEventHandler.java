package com.lingo.lingoproject.matching.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingo.lingoproject.matching.domain.event.UserProfileClickEvent;
import com.lingo.lingoproject.notification.application.FcmNotificationUseCase;
import com.lingo.lingoproject.shared.domain.model.Gender;
import com.lingo.lingoproject.shared.domain.model.NotificationType;
import com.lingo.lingoproject.shared.domain.model.Profile;
import com.lingo.lingoproject.shared.domain.model.UserProfileClickHistory;
import com.lingo.lingoproject.shared.infrastructure.persistence.UserProfileClickHistoryRepository;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UserProfileClickEventHandler {
  private final FcmNotificationUseCase fcmService;
  private final UserProfileClickHistoryRepository userProfileClickHistoryRepository;
  private final ObjectMapper objectMapper;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handle(UserProfileClickEvent event) throws Exception{
    // receiver가 여성일 경우에는 메세지를 보내지 않는다.
    if (Objects.equals(Gender.FEMALE, event.getProfileUser().getGender())) return;
    // 이미 메세지를 보냈으면 중복해서 보내지 않는다.
    if (userProfileClickHistoryRepository.existsByUserAndProfileUser(event.getUser(), event.getProfileUser())) return;

    Profile senderProfile = event.getUser().getProfile();
    String senderImageUrl = senderProfile != null ? senderProfile.getImageUrl() : null;

    String params = objectMapper.writeValueAsString(Map.of("userId", String.valueOf(event.getUser().getId())));

    fcmService.sendFcmNotification(
        event.getProfileUser(),
        senderImageUrl,
        event.getUser().getNickname() + "가 회원님의 프로필을 조회했어요",
        null,
        NotificationType.PROFILE_CLICK,
        "/profile-detail",
        params
    );

    UserProfileClickHistory history = UserProfileClickHistory.builder()
        .user(event.getUser())
        .profileUser(event.getProfileUser())
        .build();
    userProfileClickHistoryRepository.save(history);
  }
}
