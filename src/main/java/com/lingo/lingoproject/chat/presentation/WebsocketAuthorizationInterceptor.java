package com.lingo.lingoproject.chat.presentation;
import com.lingo.lingoproject.chat.application.ChatService;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import com.lingo.lingoproject.shared.infrastructure.persistence.UserRepository;
import com.lingo.lingoproject.shared.security.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

/**
 * STOMP WebSocket 메시지 인증/인가 인터셉터.
 *
 * <p>HTTP 요청의 {@link com.lingo.lingoproject.shared.security.jwt.JwtAuthenticationFilter}와 달리,
 * WebSocket은 한 번 연결하면 지속적으로 메시지를 주고받습니다.
 * 이 인터셉터는 STOMP 명령어(CONNECT, SUBSCRIBE, SEND)가 처리되기 전에 실행되어
 * JWT 토큰을 검증하고 채팅방 참여 권한을 확인합니다.</p>
 *
 * <h2>STOMP 명령어별 처리</h2>
 * <ul>
 *   <li><b>CONNECT</b>: JWT 검증 후 Principal 설정. 채팅방 권한 확인은 하지 않음.</li>
 *   <li><b>SUBSCRIBE</b>: JWT 검증 + 해당 채팅방 참여자인지 확인.</li>
 *   <li><b>SEND</b>: JWT 검증 + 해당 채팅방 참여자인지 확인.</li>
 *   <li><b>DISCONNECT</b>: 검증 없이 통과. (연결 해제는 항상 허용)</li>
 * </ul>
 *
 * <h2>토큰 검증 우선순위</h2>
 * <ol>
 *   <li>STOMP 헤더의 Authorization: Bearer {token} 검증</li>
 *   <li>토큰이 없지만 이미 Principal이 세팅된 경우 (재구독 등) → 기존 Principal로 유저 조회</li>
 *   <li>둘 다 없으면 인증 실패</li>
 * </ol>
 *
 * <h2>destination 형식</h2>
 * <p>채팅방 구독 및 메시지 전송 경로: {@code /user/queue/topic/{roomId}} 또는 {@code /app/{roomId}}</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebsocketAuthorizationInterceptor implements ChannelInterceptor {
  private final ChatService chatService;
  private final UserRepository userRepository;
  private final JwtUtil jwtUtil;

  /**
   * STOMP 메시지가 채널로 전송되기 전에 호출됩니다.
   * 토큰을 검증하고 권한을 확인한 뒤 메시지를 반환합니다.
   *
   * @param message 처리할 STOMP 메시지
   * @param channel 메시지가 전송될 채널
   * @return 검증 통과 시 원본 메시지, 실패 시 {@link RingoException} 발생
   */
  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {

    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

    if (accessor == null) return message;

    // DISCONNECT 명령어는 인증 검사 없이 통과
    if (StompCommand.DISCONNECT.equals(accessor.getCommand())){
      return message;
    }

    String token = accessor.getFirstNativeHeader("Authorization");
    User user;
    /*------------------------------------토큰 검증-----------------------------------------*/
    if (token == null || !token.startsWith("Bearer ")) {
      // 토큰이 없는 경우: 이미 CONNECT 단계에서 Principal이 설정된 경우 재사용
      if (accessor.getUser() != null) user = chatService.findUserByLoginIdOrThrow(accessor.getUser().getName());
      else throw new RingoException("토큰이 없습니다.", ErrorCode.TOKEN_INVALID, HttpStatus.FORBIDDEN);
    }
    else {
      // "Bearer " 접두사(7자) 제거 후 JWT 파싱
      token = token.substring(7);

    Claims claims = jwtUtil.getClaims(token);
    user = chatService.findUserByLoginIdOrThrow(claims.getSubject());
    // STOMP 세션의 Principal을 loginId로 설정 (StompConnectionListener에서 유저 조회에 사용)
    accessor.setUser(user::getLoginId);
    }

    /*-------------------------------------------------------------------------------------*/

    /*------------------------------------토큰으로 인증----------------------------------------*/
    // SecurityContext에 인증 객체 등록
    Authentication authentication = new UsernamePasswordAuthenticationToken(user, "password", user.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(authentication);

    // accessor의 User도 Authentication 객체로 교체 (Spring의 @AuthenticationPrincipal 주입에 필요)
    accessor.setUser(authentication);
    /*--------------------------------------------------------------------------------------*/

    // CONNECT 명령어는 연결만 허용, 채팅방 권한 확인 불필요
    if (StompCommand.CONNECT.equals(accessor.getCommand())){
      return message;
    }

    String destination = accessor.getDestination();
    Long roomId = chatService.extractChatroomIdFromDestination(destination);

    // destination에서 roomId를 파싱할 수 없는 경우 (예: /user/queue 구독 등) 통과
    if (roomId == null) {
      log.info("roomId is null");
      return message;
    }

    if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())){
      // 구독 시 해당 채팅방 참여자인지 확인
      chatService.validateParticipant(roomId, user.getId());
    }
    else if (StompCommand.SEND.equals(accessor.getCommand())){
      // 메시지 전송 시 해당 채팅방 참여자인지 확인
      chatService.validateParticipant(roomId, user.getId());
    }
    log.info("no error");
    return message;
  }

}
