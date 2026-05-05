package com.lingo.lingoproject.shared.infrastructure.retry;

import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import com.lingo.lingoproject.shared.infrastructure.persistence.DeadLetterFcmMessageRepository;
import java.util.Optional;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Redis List를 활용한 재시도 큐(Retry Queue) 기반 클래스.
 *
 * <h2>목적</h2>
 * <p>FCM 푸시 알림이나 Discord 웹훅 전송에 실패했을 때,
 * 실패한 메시지를 Redis List에 적재(LPUSH)하고
 * 별도의 스케줄러({@link com.lingo.lingoproject.notification.infrastructure.retry.FcmRetryQueueService},
 * {@link com.lingo.lingoproject.shared.infrastructure.retry.DiscordRetryQueueService})가
 * 주기적으로 꺼내어(RPOP) 재시도합니다.</p>
 *
 * <h2>큐 구조 (Redis List)</h2>
 * <pre>
 * fcm::retry-queue     [payload3, payload2, payload1]  ← LPUSH로 왼쪽에 추가
 *                       RPOP으로 오른쪽(가장 먼저 들어온 것)부터 꺼냄 → FIFO
 * discord::retry-queue [payload3, payload2, payload1]
 * </pre>
 *
 * <h2>Dead Letter 처리</h2>
 * <p>재시도 횟수 초과 시 {@link com.lingo.lingoproject.shared.domain.model.DeadLetterFcmMessage}로
 * DB에 영구 저장하여 수동 처리가 가능하도록 합니다.</p>
 *
 * <h2>확장 방법</h2>
 * <p>새로운 재시도 대상을 추가하려면:
 * <ol>
 *   <li>이 클래스에 새 키 상수 추가</li>
 *   <li>{@code pushToQueue}, {@code pollFromQueue}, {@code isEmpty}에 새 케이스 추가</li>
 *   <li>해당 서비스가 이 클래스를 상속받아 스케줄러 구현</li>
 * </ol>
 * </p>
 */
@Component
public class RedisQueueService {
  /** FCM 재시도 큐 Redis 키. */
  private static final String keyForFcm = "fcm::retry-queue";

  /** Discord 웹훅 재시도 큐 Redis 키. */
  private static final String keyForDiscord = "discord::retry-queue";

  private final RedisTemplate<String, Object> redisTemplate;
  protected final DeadLetterFcmMessageRepository deadLetterFcmMessageRepository;

  public RedisQueueService(
      RedisTemplate<String, Object> redisTemplate,
      DeadLetterFcmMessageRepository deadLetterFcmMessageRepository) {
    this.redisTemplate = redisTemplate;
    this.deadLetterFcmMessageRepository = deadLetterFcmMessageRepository;
  }

  /**
   * 재시도 큐에 메시지를 추가합니다 (LPUSH — 왼쪽에 삽입).
   *
   * @param key     큐 종류 ("FCM" 또는 "DISCORD")
   * @param payload 재시도할 메시지 페이로드
   * @throws RingoException 알 수 없는 키가 전달된 경우
   */
  public void pushToQueue(String key, RedisQueueMessagePayLoad payload){
    switch (key) {
      case "FCM" -> redisTemplate.opsForList().leftPush(keyForFcm, payload);
      case "DISCORD" -> redisTemplate.opsForList().leftPush(keyForDiscord, payload);
      default -> throw new RingoException("적절하지 않은 키 값입니다.", ErrorCode.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * 재시도 큐에서 메시지를 하나 꺼냅니다 (RPOP — 오른쪽에서 꺼냄 = FIFO).
   *
   * <p>큐가 비어있으면 {@link Optional#empty()}를 반환합니다.
   * 스케줄러에서 이 메서드를 호출하여 재시도를 처리합니다.</p>
   *
   * @param key 큐 종류 ("FCM" 또는 "DISCORD")
   * @return 꺼낸 메시지 (없으면 Optional.empty())
   */
  public Optional<?> pollFromQueue(String key){
    try{
      Object log = switch (key){
        case "FCM" -> redisTemplate.opsForList().rightPop(keyForFcm);
        case "DISCORD" -> redisTemplate.opsForList().rightPop(keyForDiscord);
        default -> null;
      };
      if (log == null) return Optional.empty();
      return Optional.of(log);
    }catch (Exception e){
      throw new RingoException("오류 메세지를 cast하는데 실패하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * 큐가 비어있는지 확인합니다.
   * 스케줄러가 재시도 대상이 있는지 먼저 확인할 때 사용합니다.
   *
   * @param key 큐 종류 ("FCM" 또는 "DISCORD")
   * @return 비어있으면 true
   */
  public boolean isEmpty(String key){
    Long size = switch (key){
      case "FCM" -> redisTemplate.opsForList().size(keyForFcm);
      case "DISCORD" -> redisTemplate.opsForList().size(keyForDiscord);
      default -> null;
    };
    if (size == null){
      return false;
    }
    return size == 0;
  }
}
