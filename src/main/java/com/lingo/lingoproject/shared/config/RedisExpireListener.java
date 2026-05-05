package com.lingo.lingoproject.shared.config;

import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.domain.model.UserActivityLog;
import com.lingo.lingoproject.shared.infrastructure.persistence.UserActivityLogRepository;
import com.lingo.lingoproject.user.application.UserQueryUseCase;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * Redis Keyspace 만료 이벤트 리스너 — 유저 활동 시간을 DB에 기록합니다.
 *
 * <h2>동작 원리</h2>
 * <p>{@link RedisConfig}에서 Redis 서버의 Keyspace 이벤트({@code notify-keyspace-events=Ex})를 활성화합니다.
 * 키가 TTL 만료로 삭제될 때 {@code __keyevent@0__:expired} 채널에 이벤트가 발행되고,
 * 이 리스너의 {@link #onMessage}가 호출됩니다.</p>
 *
 * <h2>유저 활동 추적 흐름</h2>
 * <pre>
 * [API 요청 발생] (JwtAuthenticationFilter)
 *   → Redis에 "connect-app::{userId}::{시작시간}" 키를 30분 TTL로 저장/갱신
 *
 * [30분간 요청 없음 → 키 자동 만료]
 *   → Redis Keyspace 이벤트 발행
 *   → onMessage() 호출
 *   → UserActivityLog를 DB에 저장 (시작시간, 종료시간, 활동 분)
 * </pre>
 *
 * <h2>키 형식</h2>
 * <pre>
 * connect-app::{userId}::{LocalDateTime}
 * 예: connect-app::42::2024-01-15T14:30:00
 * </pre>
 *
 * <h2>종료 시간 계산 방식</h2>
 * <p>키 만료 시점에서 29분을 뺀 값을 종료 시간으로 사용합니다.
 * 30분 TTL에서 1분의 오차를 감안한 근사값입니다.
 * (정확한 마지막 요청 시간은 별도로 저장하지 않으므로 근사 계산합니다.)</p>
 */
@Slf4j
@Component
public class RedisExpireListener implements MessageListener {

  private final UserQueryUseCase userQueryUseCase;
  private final UserActivityLogRepository userActivityLogRepository;

  public RedisExpireListener(UserQueryUseCase userQueryUseCase,
      UserActivityLogRepository userActivityLogRepository) {
    this.userQueryUseCase = userQueryUseCase;
    this.userActivityLogRepository = userActivityLogRepository;
  }

  /**
   * Redis 키 만료 이벤트를 수신하여 처리합니다.
   * {@code connect-app::} 접두사를 가진 키만 처리하고 나머지는 무시합니다.
   *
   * @param message 만료된 Redis 키 이름을 담은 메시지
   * @param pattern 구독한 패턴 ({@code __keyevent@0__:expired})
   */
  @Override
  public void onMessage(Message message, byte[] pattern){
    String expiredKey = message.toString();

    // 유저 앱 활동 추적 키만 처리 (다른 키의 만료 이벤트는 무시)
    if (expiredKey.startsWith("connect-app::")){
      // 키 형식: "connect-app::{userId}::{LocalDateTime}"
      String[] parts = expiredKey.split("::");

      Long userId = Long.parseLong(parts[1]);

      User user = userQueryUseCase.findUserOrThrow(userId);

      // 활동 시작 시간: 키 생성 시 함께 저장한 LocalDateTime
      LocalDateTime startTime = LocalDateTime.parse(parts[2]);
      // 활동 종료 시간: 만료 시점 기준 29분 전 (30분 TTL 근사값)
      LocalDateTime endTime = LocalDateTime.now().minusMinutes(29);

      long minutes = ChronoUnit.MINUTES.between(startTime, endTime);

      log.info("step=유저_활동_기록, userId={}, minutes={}", userId, minutes);

      userActivityLogRepository.save(UserActivityLog.of(user, startTime, endTime,(int) minutes));
    }

  }
}
