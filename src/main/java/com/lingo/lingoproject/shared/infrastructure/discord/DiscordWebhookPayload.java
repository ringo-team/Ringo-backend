package com.lingo.lingoproject.shared.infrastructure.discord;

import com.lingo.lingoproject.shared.infrastructure.retry.RedisQueueMessagePayLoad;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Setter
public class DiscordWebhookPayload extends RedisQueueMessagePayLoad {

  public static DiscordWebhookPayload of(String content) {
    return DiscordWebhookPayload.builder()
        .content(content)
        .retryCount(0)
        .build();
  }
}
