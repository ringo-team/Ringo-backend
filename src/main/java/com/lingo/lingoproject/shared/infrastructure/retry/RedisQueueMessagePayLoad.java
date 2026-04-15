package com.lingo.lingoproject.shared.infrastructure.retry;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Getter@Setter
public class RedisQueueMessagePayLoad {
  String content;
  Integer retryCount;
}
