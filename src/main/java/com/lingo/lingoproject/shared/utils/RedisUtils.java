package com.lingo.lingoproject.shared.utils;

import com.lingo.lingoproject.user.presentation.dto.auth.DecryptKeyObject;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import com.lingo.lingoproject.matching.presentation.dto.GetUserProfileResponseDto;
import com.lingo.lingoproject.survey.presentation.dto.GetSurveyResponseDto;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Redis 데이터 접근 유틸리티. 타입 캐스팅과 예외 처리를 공통화합니다.
 *
 * <h2>관리하는 Redis 키 목록</h2>
 * <ul>
 *   <li>{@code {tokenVersionId}}: NICE 본인인증 AES 복호화 키 ({@link DecryptKeyObject}, 30분 TTL)</li>
 *   <li>{@code recommend::{userId}}: 누적 설문 기반 추천 유저 목록 캐시 (자정 만료)</li>
 *   <li>{@code recommend-for-daily-survey::{userId}}: 일일 설문 기반 추천 유저 목록 캐시 (자정 만료)</li>
 *   <li>{@code dailySurvey::{userId}}: 유저의 당일 설문 목록 캐시 (자정 만료)</li>
 * </ul>
 *
 * <h2>타입 캐스팅 주의</h2>
 * <p>Redis에서 꺼낸 값은 {@link GenericJackson2JsonRedisSerializer}로 역직렬화되지만,
 * 제네릭 타입의 경우 런타임에서 타입 정보가 지워지므로 ClassCastException이 발생할 수 있습니다.
 * 각 메서드에서 try-catch로 이를 처리합니다.</p>
 *
 * <h2>추천 캐시 전략</h2>
 * <p>추천 유저 목록은 매일 자정에 만료되도록 {@link #cacheUntilMidnight}를 사용합니다.
 * 같은 날 동일한 유저의 추천 요청이 오면 Redis 캐시에서 즉시 반환하여 DB 조회를 최소화합니다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisUtils {

  private final RedisTemplate<String, Object> redisTemplate;

  /**
   * NICE 본인인증 콜백에서 복호화에 필요한 키 정보를 조회합니다.
   *
   * <p>인증 요청 시 AES 키 정보({@link DecryptKeyObject})를 Redis에 저장하고,
   * 콜백 수신 시 이 메서드로 꺼내어 암호화된 데이터를 복호화합니다.
   * 키는 30분 TTL로 저장됩니다.</p>
   *
   * @param key NICE API의 token_version_id (Redis 키로 사용)
   * @return 복호화 키 정보 (AES 키, IV, HMAC 키 포함)
   * @throws RingoException 역직렬화 실패 또는 키가 만료된 경우
   */
  public DecryptKeyObject getDecryptKeyObject(String key){
    try {

      return  (DecryptKeyObject) redisTemplate.opsForValue().get(key);

    } catch (Exception e) {
      log.error("본인인증 api에서 복호화 정보를 역질렬화하던 중 오류가 발생하였습니다.");
      log.error("key: {}", key, e);
      throw new RingoException("cast 도중에 오류가 발생하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * 누적 설문 기반 추천 유저 목록을 Redis 캐시에서 조회합니다.
   * 캐시 키: {@code recommend::{userId}}
   *
   * <p>캐시가 없으면 null을 반환하고, 호출자(MatchService)가 DB에서 직접 조회합니다.</p>
   *
   * @param key 유저 ID (문자열)
   * @return 캐시된 추천 유저 목록 (없으면 null)
   */
  public List<GetUserProfileResponseDto> getRecommendedUserForCumulativeSurvey(String key){
    try {

      ApiListResponseDto<GetUserProfileResponseDto> savedRecommendUserList = (ApiListResponseDto<GetUserProfileResponseDto>) redisTemplate.opsForValue().get("recommend::" + key);
      return savedRecommendUserList != null ? savedRecommendUserList.getList() : null;

    } catch (Exception e) {
      log.error("추천 유저 캐시 역직렬화 실패. key: {}", key, e);
      throw new RingoException("cast 도중에 오류가 발생하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * 일일 설문 기반 추천 유저 목록을 Redis 캐시에서 조회합니다.
   * 캐시 키: {@code recommend-for-daily-survey::{userId}}
   *
   * @param key 유저 ID (문자열)
   * @return 캐시된 추천 유저 목록 (없으면 null)
   */
  public List<GetUserProfileResponseDto> getRecommendUserForDailySurvey(String key){
    try {

      ApiListResponseDto<GetUserProfileResponseDto> savedRecommendUserList = (ApiListResponseDto<GetUserProfileResponseDto>) redisTemplate.opsForValue().get("recommend-for-daily-survey::" + key);
      return savedRecommendUserList != null ? savedRecommendUserList.getList() : null;

    } catch (Exception e) {
      log.error("일일 설문 추천 캐시 역직렬화 실패. key: {}", key, e);
      throw new RingoException("cast 도중에 오류가 발생하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * 유저의 당일 설문 목록을 Redis 캐시에서 조회합니다.
   * 캐시 키: {@code dailySurvey::{userId}}
   *
   * @param key 유저 ID (문자열)
   * @return 캐시된 설문 목록 (없으면 null)
   */
  public List<GetSurveyResponseDto> getUserDailySurvey(String key){
    try {

      ApiListResponseDto<GetSurveyResponseDto> savedUserDailySurveyList = (ApiListResponseDto<GetSurveyResponseDto>) redisTemplate.opsForValue().get("dailySurvey::" + key);
      return savedUserDailySurveyList != null ? savedUserDailySurveyList.getList() : null;

    }catch (Exception e){
      log.error("일일 설문 캐시 역직렬화 실패. key: {}", key, e);
      throw new RingoException("cast 도중에 오류가 발생하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * 값을 Redis에 저장하고 다음 날 자정에 만료되도록 설정합니다.
   * 추천 유저 목록, 일일 설문 등 하루 단위로 갱신이 필요한 데이터에 사용합니다.
   *
   * <p>만료 시각은 시스템 기본 시간대({@link ZoneId#systemDefault()})를 기준으로 계산합니다.
   * 서버 시간대가 변경되면 만료 시각도 달라지므로 주의하세요.</p>
   *
   * @param key   Redis 키
   * @param value 저장할 값 (JSON으로 직렬화됨)
   */
  public void cacheUntilMidnight(String key, Object value) {

    // 다음 날 00:00:00을 Instant로 변환
    Instant expireAt = LocalDate.now()
        .plusDays(1)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant();

    log.info("{} 값 caching 유지 기간: {} 까지", value, expireAt);

    // 캐시 저장 + 만료 시각 지정
    redisTemplate.opsForValue().set(key, value);
    redisTemplate.expireAt(key, expireAt);
  }
}
