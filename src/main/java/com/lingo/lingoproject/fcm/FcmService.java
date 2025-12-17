package com.lingo.lingoproject.fcm;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.lingo.lingoproject.domain.FailedFcmMessageLog;
import com.lingo.lingoproject.domain.FcmToken;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.exception.ErrorCode;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.fcm.dto.SaveFcmTokenRequestDto;
import com.lingo.lingoproject.repository.FailedFcmMessageLogRepository;
import com.lingo.lingoproject.repository.FcmTokenRepository;
import com.lingo.lingoproject.retry.FcmRetryQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmService {

  private final FcmTokenRepository fcmTokenRepository;
  private final FailedFcmMessageLogRepository failedFcmMessageLogRepository;
  private final FcmRetryQueueService fcmRetryQueueService;

  public void refreshFcmToken(SaveFcmTokenRequestDto dto, User user){
    FcmToken fcmToken = fcmTokenRepository.findByUser(user)
        .orElseThrow(() -> new RingoException("유저의 fcm 토큰 엔티티를 찾을 수 없습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR));
    fcmToken.setToken(dto.token());
    fcmTokenRepository.save(fcmToken);
  }

  public void sendFcmNotification(User receiver, String title, String body) {
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
      log.error("step=FCM_알림_전송_실패, requestUserId={}, title={}, status=FAILED", receiver.getId(), title, e);
      FailedFcmMessageLog log = FailedFcmMessageLog.builder()
          .token(token.getToken())
          .errorMessage(e.getMessage())
          .errorCause(e.getCause() != null ? e.getCause().getMessage() : null)
          .message(body)
          .title(title)
          .retryCount(0)
          .build();
      failedFcmMessageLogRepository.save(log);
      fcmRetryQueueService.pushToQueue("FCM", log);
      throw new RingoException("fcm 메세지를 보내는데 실패하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

}
