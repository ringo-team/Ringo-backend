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

  public void saveDecryptKeyObject(String key, Object value){
    ValueOperations<String, Object> ops = redisTemplate.opsForValue();
    ops.set(key,value, 30, TimeUnit.MINUTES);
  }

  public Object getDecryptKeyObject(String key){
    return redisTemplate.opsForValue().get(key);
  }

  public void saveBlackList(String key, String value){
    ValueOperations<String, Object> ops = redisTemplate.opsForValue();
    ops.set("blacklist::" + key, value, 2, TimeUnit.DAYS);
  }

  public boolean containsBlackList(String key){
    return redisTemplate.hasKey("blacklist::" + key);
  }
}

