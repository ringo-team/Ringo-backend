package com.lingo.lingoproject.retry;

import com.lingo.lingoproject.discord.DiscordService;
import com.lingo.lingoproject.discord.dto.DiscordWebhookPayload;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.repository.DeadLetterFcmMessageRepository;
import java.util.Optional;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DiscordRetryQueueService extends RedisQueueService{

  private final DiscordService discordService;

  public DiscordRetryQueueService(
      RedisTemplate<String, Object> redisTemplate,
      DeadLetterFcmMessageRepository deadLetterFcmMessageRepository,
      DiscordService discordService){
    super(redisTemplate, deadLetterFcmMessageRepository);
    this.discordService = discordService;
  }

  @Scheduled(fixedDelay = 60000)
  public void processRetry(){
    while(!super.isEmpty("DISCORD")) {
      Optional<?> retryEntity = super.pollFromQueue("DISCORD");
      if (retryEntity.isEmpty()){
        break;
      }
      DiscordWebhookPayload payload = null;
      try {
        payload = (DiscordWebhookPayload) retryEntity.get();
        discordService.sendMessageToDiscordChannel("webhook", payload.getMessage());
      } catch (Exception e) {
        if (payload.getRetryCount() >= 3){
          throw new RingoException("디스코드 알림이 보내지지 않습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        payload.setRetryCount(payload.getRetryCount() + 1);
        super.pushToQueue("DISCORD", payload);
      }
    }
  }
}
