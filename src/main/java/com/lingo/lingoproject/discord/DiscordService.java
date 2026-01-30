package com.lingo.lingoproject.discord;

import com.lingo.lingoproject.discord.dto.DiscordWebhookPayload;
import com.lingo.lingoproject.retry.RedisQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class DiscordService {

  private final RedisQueueService redisQueueService;

  @Value("${discord.webhook.alert.url}")
  private String discordWebhookAlertUrl;

  public void sendMessageToDiscordChannel(String webhookUri, String content){
    DiscordWebhookPayload discordWebhookPayload = DiscordWebhookPayload.builder()
        .message(content)
        .retryCount(0)
        .build();
    WebClient discordWebClient = WebClient.create();
    try{
      discordWebClient.post()
          .uri(discordWebhookAlertUrl + webhookUri)
          .bodyValue(discordWebhookPayload)
          .retrieve()
          .bodyToMono(Void.class)
          .block();
    }catch (Exception e){
      redisQueueService.pushToQueue("DISCORD", discordWebhookPayload);
    }
  }
}
