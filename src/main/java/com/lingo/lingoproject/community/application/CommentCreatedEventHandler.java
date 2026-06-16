package com.lingo.lingoproject.community.application;

import com.lingo.lingoproject.community.domain.event.CommentCreatedEvent;
import com.lingo.lingoproject.notification.application.FcmNotificationUseCase;
import com.lingo.lingoproject.shared.domain.model.NotificationType;
import com.lingo.lingoproject.shared.domain.model.Profile;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class CommentCreatedEventHandler {

  private final FcmNotificationUseCase fcmService;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Async
  public void handle(CommentCreatedEvent event){
    Profile profile = event.getCommenter().getProfile();
    fcmService.sendFcmNotification(
        event.getAuthor(),
        profile != null ? profile.getImageUrl() : null,
        event.getCommenter().getNickname() + "(이)가 댓글을 달았습니다.",
        event.getContent(),
        NotificationType.COMMENT,
        "/community/" + event.getPostId(),
        null
    );
  }
}
