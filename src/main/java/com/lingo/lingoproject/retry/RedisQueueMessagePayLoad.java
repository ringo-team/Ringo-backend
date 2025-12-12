package com.lingo.lingoproject.retry;


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
  String message;
  Integer retryCount;
}
