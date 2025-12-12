package com.lingo.lingoproject.discord.dto;

import com.lingo.lingoproject.retry.RedisQueueMessagePayLoad;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Setter
public class DiscordWebhookPayload extends RedisQueueMessagePayLoad {

}
