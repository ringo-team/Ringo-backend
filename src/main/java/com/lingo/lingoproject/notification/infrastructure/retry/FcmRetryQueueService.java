package com.lingo.lingoproject.notification.infrastructure.retry;
import com.lingo.lingoproject.shared.infrastructure.retry.RedisQueueService;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.lingo.lingoproject.shared.infrastructure.discord.DiscordService;
import com.lingo.lingoproject.shared.domain.model.DeadLetterFcmMessage;
import com.lingo.lingoproject.shared.domain.model.FailedFcmMessageLog;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import com.lingo.lingoproject.shared.infrastructure.persistence.DeadLetterFcmMessageRepository;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FcmRetryQueueService extends RedisQueueService{


  private final DiscordService discordService;

  public FcmRetryQueueService(
      RedisTemplate<String, Object> redisTemplate,
      DeadLetterFcmMessageRepository deadLetterFcmMessageRepository, DiscordService discordService){
    super(redisTemplate, deadLetterFcmMessageRepository);
    this.discordService = discordService;
  }

  @Scheduled(fixedDelay = 60000)
  public void processRetry(){
    while(!super.isEmpty("FCM")){
      Optional<?> retryEntity = super.pollFromQueue("FCM");
      if (retryEntity.isEmpty()){
        break;
      }
      FailedFcmMessageLog log = null;
      try{
        log = (FailedFcmMessageLog) retryEntity.get();
        Message message = Message.builder()
            .setToken(log.getToken())
            .setNotification(
                Notification.builder()
                    .setTitle(log.getTitle())
                    .setBody(log.getContent())
                    .build()
            )
            .build();
        FirebaseMessaging.getInstance().send(message);
      }catch (Exception e){
        if (log.getRetryCount() >= 3){
          DeadLetterFcmMessage letter = DeadLetterFcmMessage.from(log);
          super.deadLetterFcmMessageRepository.save(letter);
          discordService.sendMessageToDiscordChannel("데드레터 큐에 fcm 엔티티가 쌓였습니다.");
          throw new RingoException("데드레터큐에 fcm 엔티티가 쌓였습니다.",
              ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        log.setRetryCount(log.getRetryCount() + 1);
        pushToQueue("FCM", log);
      }
    }
  }
}
