package com.lingo.lingoproject.notification.application;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.lingo.lingoproject.shared.domain.model.FailedFcmMessageLog;
import com.lingo.lingoproject.shared.domain.model.FcmToken;
import com.lingo.lingoproject.shared.domain.model.NotificationOptionOutUser;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.domain.model.NotificationType;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import com.lingo.lingoproject.notification.presentation.dto.GetNotificationResponseDto;
import com.lingo.lingoproject.notification.presentation.dto.SaveFcmTokenRequestDto;
import com.lingo.lingoproject.shared.infrastructure.persistence.FailedFcmMessageLogRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.FcmTokenRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.NotificationOptionOutUserRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.NotificationRepository;
import com.lingo.lingoproject.notification.infrastructure.retry.FcmRetryQueueService;
import com.lingo.lingoproject.shared.utils.GenericUtils;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmNotificationUseCase {

  private final FcmTokenRepository fcmTokenRepository;
  private final FailedFcmMessageLogRepository failedFcmMessageLogRepository;
  private final FcmRetryQueueService fcmRetryQueueService;
  private final NotificationOptionOutUserRepository notificationOptionOutUserRepository;
  private final NotificationRepository notificationRepository;

  private final int RETRY_COUNT = 0;

  public void refreshFcmToken(SaveFcmTokenRequestDto dto, User user){
    FcmToken fcmToken = fcmTokenRepository.findByUser(user)
        .orElseThrow(() -> new RingoException("유저의 fcm 토큰 엔티티를 찾을 수 없습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR));
    fcmToken.setToken(dto.token());
    fcmTokenRepository.save(fcmToken);
  }

  public void sendFcmNotification(User receiver, String title, String body, NotificationType type) {

    log.info("fcm message sending start");
    notificationRepository.save(com.lingo.lingoproject.shared.domain.model.Notification.of(receiver, type, title, body));

    if(notificationOptionOutUserRepository.existsByUserAndType(receiver, type)) return;

    FcmToken token = fcmTokenRepository.findByUser(receiver)
        .orElseThrow(() -> new RingoException("fcm 토큰을 찾을 수 없습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR));
    Message message = Message.builder()
        .setToken(token.getToken())
        .setNotification(
            Notification.builder()
                .setTitle(title)
                .setBody(body)
                .setImage("ImageUrl")
                .build()
        )
        .build();
    try {
      FirebaseMessaging.getInstance().send(message);
    }catch (Exception e){
      log.error("step=FCM_알림_전송_실패, requestUserId={}, title={}", receiver.getId(), title, e);
      FailedFcmMessageLog log = FailedFcmMessageLog.of(e, token.getToken(), title, body);
      failedFcmMessageLogRepository.save(log);
      fcmRetryQueueService.pushToQueue("FCM", log);
      throw new RingoException("fcm 메세지를 보내는데 실패하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  public void alterNotificationOption(User user, String notificationType){

    NotificationType type = GenericUtils.validateAndReturnEnumValue(NotificationType.values(), notificationType);

    if(notificationOptionOutUserRepository.existsByUserAndType(user, type)){
      notificationOptionOutUserRepository.deleteAllByUserAndType(user, type);
      return;
    }
    saveNotificationOptionOutUser(user, type);
  }

  public void saveNotificationOptionOutUser(User user, NotificationType type){
    notificationOptionOutUserRepository.save(NotificationOptionOutUser.of(user, type));
  }

  public List<GetNotificationResponseDto> getNotificationMessage(User user){
    List<com.lingo.lingoproject.shared.domain.model.Notification> notifications = notificationRepository.findAllByUser(user);
    return notifications.stream()
        .map(n -> new GetNotificationResponseDto(n.getTitle(), n.getMessage()))
        .toList();
  }
}

