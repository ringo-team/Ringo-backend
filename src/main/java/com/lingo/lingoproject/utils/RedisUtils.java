package com.lingo.lingoproject.utils;

import com.lingo.lingoproject.auth.dto.DecryptKeyObject;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.match.dto.GetUserProfileResponseDto;
import com.lingo.lingoproject.survey.dto.GetSurveyResponseDto;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisUtils {

  private final RedisTemplate<String, Object> redisTemplate;

  public void saveDecryptKeyObject(String key, Object value){
    ValueOperations<String, Object> ops = redisTemplate.opsForValue();
    ops.set(key,value, 30, TimeUnit.MINUTES);
  }

  public DecryptKeyObject getDecryptKeyObject(String key){
    DecryptKeyObject savedDecryptedKey = null;
    try {
      savedDecryptedKey = (DecryptKeyObject) redisTemplate.opsForValue().get(key);
    } catch (Exception e) {
      throw new RingoException("cast 도중에 오류가 발생하였습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return savedDecryptedKey;
  }

  public void saveSelfAuthCompletionMark(String userId){
    ValueOperations<String, Object> ops = redisTemplate.opsForValue();
    ops.set("self-auth::" + userId,true, 30, TimeUnit.MINUTES);
  }

  public boolean isCompleteSelfAuth(String userId){
    return redisTemplate.hasKey("self-auth::" + userId);
  }

  public void saveLogoutUserList(String key, String value){
    ValueOperations<String, Object> ops = redisTemplate.opsForValue();
    ops.set("logoutUser::" + key, value, 1, TimeUnit.DAYS);
  }

  public boolean containsLogoutUserList(String key){
    return redisTemplate.hasKey("logoutUser::" + key);
  }

  public void saveRecommendUser(String key, Object value){
    cacheUntilMidnight("recommend::" + key, value);
  }

  public boolean containsRecommendUser(String key){
    return redisTemplate.hasKey("recommend::" + key);
  }

  public List<GetUserProfileResponseDto> getRecommendUser(String key){
    JsonListWrapper<GetUserProfileResponseDto> savedRecommendUserList = null;
    try {
      savedRecommendUserList = (JsonListWrapper<GetUserProfileResponseDto>) redisTemplate.opsForValue().get("recommend::" + key);
    } catch (Exception e) {
      throw new RingoException("cast 도중에 오류가 발생하였습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return savedRecommendUserList.getList();
  }

  public void saveRecommendUserForDailySurvey(String key, Object value){
    cacheUntilMidnight("recommend-for-daily-survey::" + key, value);
  }

  public boolean containsRecommendUserForDailySurvey(String key){
    return redisTemplate.hasKey("recommend-for-daily-survey::" + key);
  }

  public List<GetUserProfileResponseDto> getRecommendUserForDailySurvey(String key){
    JsonListWrapper<GetUserProfileResponseDto> savedRecommendUserList = null;
    try {
      savedRecommendUserList = (JsonListWrapper<GetUserProfileResponseDto>) redisTemplate.opsForValue().get("recommend-for-daily-survey::" + key);
    } catch (Exception e) {
      throw new RingoException("cast 도중에 오류가 발생하였습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return savedRecommendUserList.getList();
  }

  public void saveUserDailySurvey(String key, List<GetSurveyResponseDto> value){
    cacheUntilMidnight("dailySurvey::" + key, new JsonListWrapper<>(value));
  }

  public boolean containsUserDailySurvey(String key){
    return redisTemplate.hasKey("dailySurvey::" + key);
  }

  public List<GetSurveyResponseDto> getUserDailySurvey(String key){
    JsonListWrapper<GetSurveyResponseDto> savedUserDailySurveyList = null;
    try {
      savedUserDailySurveyList = (JsonListWrapper<GetSurveyResponseDto>) redisTemplate.opsForValue().get("dailySurvey::" + key);
    }catch (Exception e){
      throw new RingoException("cast 도중에 오류가 발생하였습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return savedUserDailySurveyList.getList();
  }

  public void suspendUser(Long userId, int day){
    ValueOperations<String, Object> ops = redisTemplate.opsForValue();
    ops.set("suspension::" + userId, true, day, TimeUnit.DAYS);
  }

  public boolean isSuspendedUser(Long userId){
    return redisTemplate.hasKey("suspension::" + userId);
  }

  public Set<String> getSuspendedUser(){
    return redisTemplate.keys("suspension::*");
  }

  public void cacheUntilMidnight(String key, Object value) {

    Instant expireAtUtc = LocalDate.now()
        .plusDays(1)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant();

    //  캐시 저장 + 만료 시각 지정
    redisTemplate.opsForValue().set(key, value);
    redisTemplate.expireAt(key, expireAtUtc);
  }
}

