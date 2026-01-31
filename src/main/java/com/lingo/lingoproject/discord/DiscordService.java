package com.lingo.lingoproject.discord;

import com.lingo.lingoproject.discord.dto.DiscordWebhookPayload;
import com.lingo.lingoproject.retry.RedisQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiscordService {

  private final RedisQueueService redisQueueService;

  @Value("${discord.webhook.alert.url}")
  private String discordWebhookAlertUrl;

  public void sendMessageToDiscordChannel(String content){
    DiscordWebhookPayload discordWebhookPayload = DiscordWebhookPayload.builder()
        .content(content)
        .retryCount(0)
        .build();
    WebClient discordWebClient = WebClient.create();
    try{
      discordWebClient.post()
          .uri(discordWebhookAlertUrl)
          .bodyValue(discordWebhookPayload)
          .retrieve()
          .bodyToMono(Void.class)
          .block();
    }catch (Exception e){
      log.error("에러", e);
      redisQueueService.pushToQueue("DISCORD", discordWebhookPayload);
    }
  }
}
