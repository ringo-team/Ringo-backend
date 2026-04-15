package com.lingo.lingoproject.common.discord.dto;

import com.lingo.lingoproject.common.retry.RedisQueueMessagePayLoad;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Setter
public class DiscordWebhookPayload extends RedisQueueMessagePayLoad {

}
