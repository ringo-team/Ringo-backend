package com.lingo.lingoproject.chat.infrastructure.config;

import com.lingo.lingoproject.chat.presentation.WebsocketAuthorizationInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP 기반 WebSocket 메시지 브로커 설정 클래스.
 *
 * <h2>STOMP 프로토콜 개요</h2>
 * <p>STOMP(Simple Text Oriented Messaging Protocol)는 WebSocket 위에서 동작하는 메시징 프로토콜입니다.
 * 연결(CONNECT), 구독(SUBSCRIBE), 메시지 전송(SEND), 연결 해제(DISCONNECT) 등의 명령어를 제공합니다.</p>
 *
 * <h2>채팅 흐름</h2>
 * <pre>
 * [클라이언트]                          [서버]
 *   CONNECT  → ws://host/ws        → WebSocket 핸드셰이크
 *   SUBSCRIBE → /user/queue/topic/{roomId}   → 채팅방 메시지 수신 채널 구독
 *   SEND     → /app/{roomId}       → ChatController.sendMessage() 호출
 *                                  → ChatService가 처리 후
 *                                  → /user/queue/topic/{roomId}로 메시지 브로드캐스트
 * </pre>
 *
 * <h2>주요 경로 정리</h2>
 * <ul>
 *   <li>{@code /ws}: WebSocket 연결 엔드포인트 (HTTP → WebSocket 업그레이드)</li>
 *   <li>{@code /app}: 클라이언트 → 서버 메시지 전송 prefix (ApplicationDestinationPrefix)</li>
 *   <li>{@code /user/queue}: 서버 → 특정 클라이언트 메시지 전송 prefix (UserDestinationPrefix)</li>
 * </ul>
 *
 * <h2>인증</h2>
 * <p>WebSocket 연결 및 STOMP 명령어 처리 시 {@link WebsocketAuthorizationInterceptor}가
 * Authorization 헤더의 JWT 토큰을 검증합니다.
 * HTTP Security 필터 체인은 WebSocket 핸드셰이크 단계에서만 적용되므로,
 * STOMP 레벨 인증은 이 인터셉터에서 별도로 처리해야 합니다.</p>
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final WebsocketAuthorizationInterceptor webSocketAuthorizationInterceptor;

  /**
   * 메시지 브로커를 설정합니다.
   *
   * <p>In-Memory 브로커를 사용합니다 (별도의 RabbitMQ/ActiveMQ 없이 스프링 내장).
   * 트래픽이 증가하면 외부 메시지 브로커로 전환을 고려하세요.</p>
   *
   * <ul>
   *   <li>{@code setApplicationDestinationPrefixes("/app")}: 클라이언트가 {@code /app/...}으로 보내는
   *       메시지는 {@code @MessageMapping} 메서드로 라우팅됩니다.</li>
   *   <li>{@code setUserDestinationPrefix("/user/queue")}: {@code SimpMessagingTemplate.convertAndSendToUser()}
   *       를 통해 특정 유저에게 메시지를 전달할 때 사용하는 prefix입니다.</li>
   * </ul>
   */
  @Override
  public void configureMessageBroker(MessageBrokerRegistry config){

    /*
     *  메세지를 받을 경로 (메세지의 목적지)
     *  클라이언트 전송: /app/{roomId}
     *
     *  웹소켓 구독 경로 (서버 → 클라이언트)
     *  /user/queue/topic/{roomId}
     */
    config.setApplicationDestinationPrefixes("/app");

    config.setUserDestinationPrefix("/user/queue");
  }

  /**
   * STOMP WebSocket 엔드포인트를 등록합니다.
   *
   * <p>클라이언트는 {@code ws://host/ws}로 WebSocket 연결을 시작합니다.
   * SockJS는 현재 사용하지 않습니다 — 네이티브 WebSocket만 지원합니다.
   * SockJS가 필요하다면 {@code .withSockJS()}를 추가하세요.</p>
   */
  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry){
    /*
     *  URL/ws 로 socket 연결
     *  cors 설정
     */
    registry.addEndpoint("/ws")
        .setAllowedOriginPatterns("*");
  }

  /**
   * 클라이언트 인바운드 채널에 인터셉터를 등록합니다.
   *
   * <p>{@link WebsocketAuthorizationInterceptor}가 CONNECT, SUBSCRIBE, SEND 명령어마다
   * JWT 토큰을 검증하고 채팅방 참여 권한을 확인합니다.</p>
   */
  @Override
  public void configureClientInboundChannel(ChannelRegistration registration){
    registration.interceptors(webSocketAuthorizationInterceptor);
  }
}
