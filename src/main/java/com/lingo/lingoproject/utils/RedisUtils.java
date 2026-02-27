package com.lingo.lingoproject.utils;

import com.lingo.lingoproject.auth.dto.DecryptKeyObject;
import com.lingo.lingoproject.exception.ErrorCode;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.match.dto.GetUserProfileResponseDto;
import com.lingo.lingoproject.survey.dto.GetSurveyResponseDto;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisUtils {

  private final RedisTemplate<String, Object> redisTemplate;


  public DecryptKeyObject getDecryptKeyObject(String key){
    try {

      return  (DecryptKeyObject) redisTemplate.opsForValue().get(key);

    } catch (Exception e) {
      log.error("본인인증 api에서 복호화 정보를 역질렬화하던 중 오류가 발생하였습니다.");
      log.error("key: {}", key, e);
      throw new RingoException("cast 도중에 오류가 발생하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  public List<GetUserProfileResponseDto> getRecommendedUserForCumulativeSurvey(String key){
    try {

      ApiListResponseDto<GetUserProfileResponseDto> savedRecommendUserList = (ApiListResponseDto<GetUserProfileResponseDto>) redisTemplate.opsForValue().get("recommend::" + key);
      return savedRecommendUserList != null ? savedRecommendUserList.getList() : null;

    } catch (Exception e) {
      log.error("추천 유저 캐시 역직렬화 실패. key: {}", key, e);
      throw new RingoException("cast 도중에 오류가 발생하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }



  public List<GetUserProfileResponseDto> getRecommendUserForDailySurvey(String key){
    try {

      ApiListResponseDto<GetUserProfileResponseDto> savedRecommendUserList = (ApiListResponseDto<GetUserProfileResponseDto>) redisTemplate.opsForValue().get("recommend-for-daily-survey::" + key);
      return savedRecommendUserList != null ? savedRecommendUserList.getList() : null;

    } catch (Exception e) {
      log.error("일일 설문 추천 캐시 역직렬화 실패. key: {}", key, e);
      throw new RingoException("cast 도중에 오류가 발생하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }


  public List<GetSurveyResponseDto> getUserDailySurvey(String key){
    try {

      ApiListResponseDto<GetSurveyResponseDto> savedUserDailySurveyList = (ApiListResponseDto<GetSurveyResponseDto>) redisTemplate.opsForValue().get("dailySurvey::" + key);
      return savedUserDailySurveyList != null ? savedUserDailySurveyList.getList() : null;

    }catch (Exception e){
      log.error("일일 설문 캐시 역직렬화 실패. key: {}", key, e);
      throw new RingoException("cast 도중에 오류가 발생하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
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
