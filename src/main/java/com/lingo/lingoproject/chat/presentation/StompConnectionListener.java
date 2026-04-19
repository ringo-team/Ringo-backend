package com.lingo.lingoproject.chat.presentation;

import com.lingo.lingoproject.chat.application.ChatService;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import com.lingo.lingoproject.shared.infrastructure.persistence.UserRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class StompConnectionListener {

  private final RedisTemplate<String, Object> redisTemplate;
  private final UserRepository userRepository;
  private final ChatService chatService;

  @EventListener
  public void onConnected(SessionSubscribeEvent event){
    try {

      // 접속 유저 조회
      StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
      if (accessor.getUser() == null) return;
      String loginId = accessor.getUser().getName();
      User user = chatService.findUserByLoginIdOrThrow(loginId);

      // 접속 중인 채팅방 조회
      String destination = accessor.getDestination();
      Long roomId = chatService.extractRoomIdFromDestination(destination);
      if (roomId == null) return;

      // 유저 채팅방 레디스에 저장
      log.info("step=채팅방_입장, userId={}, chatroomId={}", user.getId(), roomId);
      redisTemplate.opsForValue().set("connect::" + user.getId() + "::" + roomId, true);

    } catch (Exception e) {
      log.error("step=채팅방_입장_오류, 레디스 연결 상태 저장 중 오류 발생");
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("웹소켓 연결 상태를 레디스에 저장하던 중 오류가 발생하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }


  @EventListener
  public void onDisconnected(SessionDisconnectEvent event){
    try {

      log.info("step=WebSocket_연결_해제_시작");

      // 유저 조회
      StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
      if (accessor.getUser() == null) {
        log.info("step=WebSocket_연결_해제_유저_없음");
        return;
      }

      String username = accessor.getUser().getName();
      User user = chatService.findUserByLoginIdOrThrow(username);

      // 유저 id 로 저장된 키 모두 삭제
      Set<String> keyset = redisTemplate.keys("connect::" + user.getId() + "*");
      if (keyset.isEmpty()){
        log.info("redis key cannot found");
        throw new RingoException("redis key cannot found", ErrorCode.NOT_FOUND, HttpStatus.INTERNAL_SERVER_ERROR);
      }
      for (String key : keyset) {
        log.info("step=WebSocket_연결_키_삭제, key={}", key);
        redisTemplate.delete(key);
      }

    }catch (Exception e){
      log.error("step=채팅방_퇴장_오류, 레디스 연결 정보 삭제 중 오류 발생");
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("웹소켓 연결을 끊고 레디스에 연결 정보를 삭제하던 중 오류가 발생하였습니다.",
          ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
