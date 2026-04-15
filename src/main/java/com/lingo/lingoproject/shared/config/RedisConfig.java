package com.lingo.lingoproject.shared.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 연결 및 기능 설정 클래스.
 *
 * <h2>Redis 사용 목적</h2>
 * <ul>
 *   <li><b>JWT 블랙리스트</b>: 로그아웃된 토큰 차단 ({@code logoutUser::{jti}})</li>
 *   <li><b>Refresh 토큰 저장</b>: {@code redis::refresh::{loginId}}</li>
 *   <li><b>계정 정지</b>: 일시 정지 유저 ({@code suspension::{userId}}, TTL 설정)</li>
 *   <li><b>본인인증 세션</b>: NICE 암복호화 키 임시 저장 ({@code {tokenVersionId}}, 30분 TTL)</li>
 *   <li><b>WebSocket 연결 상태</b>: 채팅방 입장 유저 ({@code connect::{userId}::{roomId}})</li>
 *   <li><b>앱 활동 추적</b>: 유저 접속 활동 ({@code connect-app::{userId}::{timestamp}}, 30분 TTL)</li>
 *   <li><b>재시도 큐</b>: FCM/Discord 실패 메시지 ({@code fcm::retry-queue}, {@code discord::retry-queue})</li>
 * </ul>
 *
 * <h2>직렬화 전략</h2>
 * <ul>
 *   <li>Key: {@link StringRedisSerializer} — 문자열 그대로 저장 (가독성)</li>
 *   <li>Value: {@link GenericJackson2JsonRedisSerializer} — JSON으로 직렬화 (타입 정보 포함)</li>
 * </ul>
 *
 * <h2>Keyspace 이벤트 (만료 감지)</h2>
 * <p>{@code enableKeyspaceEvents}에서 Redis 서버에 {@code notify-keyspace-events=Ex} 설정을 주입합니다.
 * "E"는 Keyspace 이벤트, "x"는 expired 이벤트를 의미합니다.
 * 키가 만료될 때 {@code __keyevent@0__:expired} 채널로 이벤트가 발행되고,
 * {@link RedisExpireListener}가 이를 수신하여 유저 활동 로그를 DB에 저장합니다.</p>
 *
 * <h2>연결 라이브러리</h2>
 * Lettuce를 사용합니다. Jedis 대비 비동기/논블로킹을 지원하며 Spring Boot 기본값입니다.
 */
@Configuration
public class RedisConfig {

  /** Redis 서버 호스트 (application.yml: spring.data.redis.host). */
  @Value("${spring.data.redis.host}")
  private String redisHost;

  /** Redis 서버 포트 (application.yml: spring.data.redis.port). 기본값 6379. */
  @Value("${spring.data.redis.port}")
  private int redisPort;

  /** Redis 인증 비밀번호 (application.yml: spring.data.redis.password). */
  @Value("${spring.data.redis.password}")
  private String redisPassword;

  /**
   * Lettuce 기반 Redis 연결 팩토리를 생성합니다.
   * Standalone 모드(단일 Redis 서버)를 사용합니다.
   */
  @Bean
  public RedisConnectionFactory redisConnectionFactory(){
    RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
    configuration.setHostName(redisHost);
    configuration.setPort(redisPort);
    configuration.setPassword(redisPassword);
    return new LettuceConnectionFactory(configuration);
  }

  /**
   * 범용 RedisTemplate을 생성합니다.
   * Key는 String, Value는 JSON 직렬화를 사용합니다.
   * 프로젝트 전체에서 {@code @Autowired RedisTemplate<String, Object>}로 주입하여 사용합니다.
   */
  @Bean
  public RedisTemplate<String, Object> redisTemplate(
      RedisConnectionFactory redisConnectionFactory
  ) {
    RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
    redisTemplate.setConnectionFactory(redisConnectionFactory);
    // 키는 문자열 그대로 저장 (예: "logoutUser::uuid-1234")
    redisTemplate.setKeySerializer(new StringRedisSerializer());
    // 값은 JSON으로 직렬화 (타입 정보가 포함되어 역직렬화 시 올바른 타입으로 복원됨)
    redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
    return redisTemplate;
  }

  /**
   * Redis Keyspace 이벤트 리스너 컨테이너를 생성합니다.
   * {@code __keyevent@0__:expired} 패턴을 구독하여
   * 키가 만료될 때 {@link RedisExpireListener#onMessage}를 호출합니다.
   *
   * <p>현재 DB 인덱스 0번을 사용합니다. DB 번호가 달라지면 패턴도 수정 필요합니다.</p>
   */
  @Bean
  RedisMessageListenerContainer container(
      RedisConnectionFactory redisConnectionFactory,
      RedisExpireListener listenerAdapter
      ){
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(redisConnectionFactory);

    // 0번 DB의 expired 이벤트 구독
    container.addMessageListener(
        listenerAdapter, new PatternTopic("__keyevent@0__:expired")
    );
    return container;
  }

  /**
   * 애플리케이션 시작 시 Redis 서버에 Keyspace 이벤트 알림을 활성화합니다.
   * "Ex" = Expired 이벤트(x)를 Keyspace 이벤트(E) 방식으로 발행.
   *
   * <p>주의: Redis 서버 설정 파일(redis.conf)에서 직접 설정하는 것이 권장되지만,
   * 이 방식으로 런타임에 동적으로 설정할 수도 있습니다.
   * 재시작 시 초기화되므로 InitializingBean으로 매번 설정합니다.</p>
   */
  @Bean
  public InitializingBean enableKeyspaceEvents(RedisConnectionFactory factory) {
    return () -> {
      try (RedisConnection connection = factory.getConnection()) {
        connection.setConfig("notify-keyspace-events", "Ex");
      }
    };
  }
}
