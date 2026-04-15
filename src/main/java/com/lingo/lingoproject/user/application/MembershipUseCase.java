package com.lingo.lingoproject.user.application;

import com.lingo.lingoproject.shared.domain.model.MemberShipLog;
import com.lingo.lingoproject.shared.domain.model.MemberShipType;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.infrastructure.persistence.MemberShipLogRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 멤버십(커뮤니티 패스) 구독 Use Case
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MembershipUseCase {

  private final MemberShipLogRepository memberShipLogRepository;
  private final RedisTemplate<String, Object> redisTemplate;

  public void saveMembership(int duration, User user) {
    redisTemplate.opsForValue().set("membership::" + user.getId(), true, Duration.of(duration, ChronoUnit.DAYS));
    LocalDateTime endTime = LocalDateTime.now().plusDays(duration);
    memberShipLogRepository.save(MemberShipLog.of(user, MemberShipType.COMMUNITY_PASS, endTime));
  }
}