package com.lingo.lingoproject.retry;

import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.repository.DeadLetterFcmMessageRepository;
import java.util.Optional;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class RedisQueueService {
  private static final String keyForFcm = "fcm::retry-queue";
  private static final String keyForDiscord = "discord::retry-queue";

  private final RedisTemplate<String, Object> redisTemplate;
  protected final DeadLetterFcmMessageRepository deadLetterFcmMessageRepository;

  public RedisQueueService(
      RedisTemplate<String, Object> redisTemplate,
      DeadLetterFcmMessageRepository deadLetterFcmMessageRepository) {
    this.redisTemplate = redisTemplate;
    this.deadLetterFcmMessageRepository = deadLetterFcmMessageRepository;
  }

  public void pushToQueue(String key, RedisQueueMessagePayLoad payload){
    switch (key) {
      case "FCM" -> redisTemplate.opsForList().leftPush(keyForFcm, payload);
      case "DISCORD" -> redisTemplate.opsForList().rightPush(keyForDiscord, payload);
      default -> throw new RingoException("적절하지 않은 키 값입니다.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  public Optional<?> pollFromQueue(String key){
    try{
      Object log = switch (key){
        case "FCM" -> redisTemplate.opsForList().rightPop(keyForFcm);
        case "DISCORD" -> redisTemplate.opsForList().rightPop(keyForDiscord);
        default -> null;
      };
      if (log == null) return Optional.empty();
      return Optional.of(log);
    }catch (Exception e){
      throw new RingoException("오류 메세지를 cast하는데 실패하였습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  public boolean isEmpty(String key){
    Long size = switch (key){
      case "FCM" -> redisTemplate.opsForList().size(keyForFcm);
      case "DISCORD" -> redisTemplate.opsForList().size(keyForDiscord);
      default -> null;
    };
    if (size == null){
      return false;
    }
    return size == 0;
  }
}
