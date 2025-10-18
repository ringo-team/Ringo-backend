package com.lingo.lingoproject.chat;

import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.repository.UserRepository;
import com.lingo.lingoproject.security.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebsocketAuthorizationInterceptor implements ChannelInterceptor {
  private final ChatService chatService;
  private final UserRepository userRepository;
  private final JwtUtil jwtUtil;

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

    if (accessor == null) return message;

    String token = accessor.getFirstNativeHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
      throw new RingoException("토큰이 없습니다.", HttpStatus.FORBIDDEN);
    }
    token = token.substring(7);
    if(!jwtUtil.isValidToken(token)){
      throw new RingoException("유효한 토큰이 아닙니다.", HttpStatus.FORBIDDEN);
    }
    Claims claims = jwtUtil.getClaims(token);
    User user = userRepository.findByEmail(claims.getSubject())
            .orElseThrow(() -> new RingoException("유효한 토큰이 아닙니다.", HttpStatus.FORBIDDEN));

    Authentication authentication = new UsernamePasswordAuthenticationToken(user, "password", user.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(authentication);
    accessor.setUser(authentication);

    String destination = accessor.getDestination();
    Long roomId = extractRoomId(destination);

    if(StompCommand.CONNECT.equals(accessor.getCommand()) || StompCommand.DISCONNECT.equals(accessor.getCommand())){
      return message;
    }

    if (roomId == null) {
      throw new RingoException("채팅방에 관한 정보가 없습니다.", HttpStatus.BAD_REQUEST);
    }

    if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())){
      if(!chatService.isMemberInChatroom(roomId, user.getId())){
        throw new RingoException("구독 권한이 없습니다.", HttpStatus.FORBIDDEN);
      }
    }
    else if (StompCommand.SEND.equals(accessor.getCommand())){
      if(!chatService.isMemberInChatroom(roomId, user.getId())){
        throw new RingoException("메세지 전달 권한이 없습니다.", HttpStatus.FORBIDDEN);
      }
    }
    return message;
  }

  // 목적지 패턴이 /topic/{id}일 때 id만 가져옴
  private Long extractRoomId(String destination) {
    if (destination == null) return null;
    String[] parts = destination.split("/");
    if (parts.length == 0) return null;
    try {
      return Long.valueOf(parts[parts.length - 1]);
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
