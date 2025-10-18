package com.lingo.lingoproject.config;

import com.lingo.lingoproject.chat.WebsocketAuthorizationInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final WebsocketAuthorizationInterceptor webSocketAuthorizationInterceptor;

  @Override
  public void configureMessageBroker(MessageBrokerRegistry config){
//    /*
//     * 해당 경로(/topic/{roomId})로
//     * 토픽을 구독하는 클라이언트에게
//     * 메세지 전달
//     */
//    config.enableSimpleBroker("/broker");

    /*
     *  메세지를 받을 경로 (메세지의 목적지)
     *  /app/message-room-id/~
     */
    config.setApplicationDestinationPrefixes("/app");

    config.setUserDestinationPrefix("/user/queue");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry){
    /*
     *  URL/ws 로 socket 연결
     *  cors 설정
     */
    registry.addEndpoint("/ws")
        .setAllowedOriginPatterns("*");
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration){
    registration.interceptors(webSocketAuthorizationInterceptor);
  }
}
