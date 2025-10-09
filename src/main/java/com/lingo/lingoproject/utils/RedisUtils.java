package com.lingo.lingoproject.utils;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisUtils {

  private final RedisTemplate<String, Object> redisTemplate;

  public void save(String key, Object value){
    ValueOperations<String, Object> ops = redisTemplate.opsForValue();
    ops.set(key,value, 30*60*1000, TimeUnit.MILLISECONDS);
  }

  public Object get(String key){
    return redisTemplate.opsForValue().get(key);
  }
}
