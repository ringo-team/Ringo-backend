package com.lingo.lingoproject.discord;

import com.lingo.lingoproject.discord.dto.DiscordWebhookPayload;
import com.lingo.lingoproject.retry.RedisQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class DiscordService {

  private final WebClient discordWebClient;
  private final RedisQueueService redisQueueService;

  public void sendMessageToDiscordChannel(String webhookUri, String content){
    DiscordWebhookPayload discordWebhookPayload = DiscordWebhookPayload.builder()
        .message(content)
        .retryCount(0)
        .build();
    try{
      discordWebClient.post()
          .uri(webhookUri)
          .bodyValue(discordWebhookPayload)
          .retrieve()
          .bodyToMono(Void.class)
          .block();
    }catch (Exception e){
      redisQueueService.pushToQueue("DISCORD", discordWebhookPayload);
    }
  }
}
