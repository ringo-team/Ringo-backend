package com.lingo.lingoproject.chat;

import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.exception.ErrorCode;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.repository.UserRepository;
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

  @EventListener
  public void onConnected(SessionSubscribeEvent event){
    try {

      // 접속 유저 조회
      StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
      if (accessor.getUser() == null) return;
      String loginId = accessor.getUser().getName();
      User user = userRepository.findByEmail(loginId).orElseThrow(() -> new RingoException(
              "세션에 해당하는 유저를 찾을 수 없습니다.", ErrorCode.NOT_FOUND_USER, HttpStatus.BAD_REQUEST));

      // 접속 중인 채팅방 조회
      String destination = accessor.getDestination();
      Long roomId = extractRoomIdFromDestination(destination);
      if (roomId == null) return;

      // 유저 채팅방 레디스에 저장
      log.info("userId={}, chatroomId={}, step=채팅방_입장, status=SUCCESS", user.getId(), roomId);
      ValueOperations<String, Object> ops = redisTemplate.opsForValue();
      ops.set("connect::" + user.getId() + "::" + roomId, true);

    } catch (Exception e) {
      log.error("웹소켓 연결 상태를 레디스에 저장하던 중 오류가 발생하였습니다.");
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("웹소켓 연결 상태를 레디스에 저장하던 중 오류가 발생하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private Long extractRoomIdFromDestination(String destination){
    if (destination == null) return null;
    String lastWord = destination.substring(destination.lastIndexOf("/") + 1);
    long roomId;
    try{
      roomId = Long.parseLong(lastWord);
    }catch (Exception e){
      return null;
    }
    return roomId;
  }

  @EventListener
  public void onDisconnected(SessionDisconnectEvent event){
    try {

      // 유저 조회
      StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
      if (accessor.getUser() == null) return;
      String username = accessor.getUser().getName();
      User user = userRepository.findByEmail(username).orElseThrow(() ->
          new RingoException("세션에 해당하는 유저를 찾을 수 없습니다.", ErrorCode.NOT_FOUND_USER, HttpStatus.BAD_REQUEST));

      // 유저 id 로 저장된 키 모두 삭제
      Set<String> keyset = redisTemplate.keys("connect::" + user.getId());
      for (String key : keyset) {
        redisTemplate.delete(key);
      }

    }catch (Exception e){
      log.error("웹소켓 연결을 끊고 레디스에 연결 정보를 삭제하던 중 오류가 발생하였습니다.");
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("웹소켓 연결을 끊고 레디스에 연결 정보를 삭제하던 중 오류가 발생하였습니다.",
          ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
