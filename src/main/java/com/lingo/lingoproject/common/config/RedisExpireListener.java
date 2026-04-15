package com.lingo.lingoproject.common.config;

import com.lingo.lingoproject.common.exception.ErrorCode;
import com.lingo.lingoproject.common.exception.RingoException;
import com.lingo.lingoproject.db.domain.User;
import com.lingo.lingoproject.db.domain.UserActivityLog;
import com.lingo.lingoproject.db.repository.UserActivityLogRepository;
import com.lingo.lingoproject.db.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RedisExpireListener implements MessageListener {

  private final UserRepository userRepository;
  private final UserActivityLogRepository userActivityLogRepository;

  public RedisExpireListener(UserRepository userRepository,
      UserActivityLogRepository userActivityLogRepository) {
    this.userRepository = userRepository;
    this.userActivityLogRepository = userActivityLogRepository;
  }

  @Override
  public void onMessage(Message message, byte[] pattern){
    String expiredKey = message.toString();

    if (expiredKey.startsWith("connect-app::")){
      String[] parts = expiredKey.split("::");

      Long userId = Long.parseLong(parts[1]);

      User user = userRepository.findById(userId).orElseThrow(
          () -> new RingoException("찾을 수 없는 유저", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST)
      );

      LocalDateTime startTime = LocalDateTime.parse(parts[2]);
      LocalDateTime endTime = LocalDateTime.now().minusMinutes(29);

      long minutes = ChronoUnit.MINUTES.between(startTime, endTime);

      log.info("""
            활동 중지 유저: {},
            활동 기간: {}
            """,
          userId,
          minutes);

      UserActivityLog logEntity = UserActivityLog.builder()
          .user(user)
          .start(startTime)
          .end(endTime)
          .activityMinuteDuration(minutes)
          .build();

      userActivityLogRepository.save(logEntity);
    }

  }
}
