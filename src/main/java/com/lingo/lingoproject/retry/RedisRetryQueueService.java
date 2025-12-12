package com.lingo.lingoproject.retry;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.lingo.lingoproject.domain.DeadLetterFcmMessage;
import com.lingo.lingoproject.domain.FailedFcmMessageLog;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.repository.DeadLetterFcmMessageRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisRetryQueueService {

  private static final String key = "fcm::retry-queue";
  private final RedisTemplate<Object, Object> redisTemplate;
  private final DeadLetterFcmMessageRepository deadLetterFcmMessageRepository;

  public void pushToQueue(FailedFcmMessageLog failedFcmMessageLog){
    redisTemplate.opsForList().leftPush(key, failedFcmMessageLog);
  }

  public Optional<FailedFcmMessageLog> pollFromQueue(){
    try{
      FailedFcmMessageLog log = (FailedFcmMessageLog) redisTemplate.opsForList().rightPop(key);
      if (log == null){
        return Optional.empty();
      }
      return Optional.of(log);
    }catch (Exception e){
      throw new RingoException("fcm 오류 메세지를 cast하는데 실패하였습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  public boolean isEmpty(){
    Long size = redisTemplate.opsForList().size(key);
    if (size == null){
      return false;
    }
    return size == 0;
  }

  @Scheduled(fixedDelay = 60000)
  public void processRetry(){
    while(!this.isEmpty()){
      Optional<FailedFcmMessageLog> retryEntity = this.pollFromQueue();
      if (retryEntity.isEmpty()){
        break;
      }
      FailedFcmMessageLog log = retryEntity.get();
      try{
        Message message = Message.builder()
            .setToken(log.getToken())
            .setNotification(
                Notification.builder()
                    .setTitle(log.getTitle())
                    .setBody(log.getMessage())
                    .build()
            )
            .build();
        FirebaseMessaging.getInstance().send(message);
      }catch (Exception e){
        if (log.getRetryCount() >= 3){
          DeadLetterFcmMessage letter = DeadLetterFcmMessage.from(log);
          deadLetterFcmMessageRepository.save(letter);
          throw new RingoException("데드레터큐에 fcm 엔티티가 쌓였습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        log.setRetryCount(log.getRetryCount() + 1);
        pushToQueue(log);
      }
    }
  }
}
