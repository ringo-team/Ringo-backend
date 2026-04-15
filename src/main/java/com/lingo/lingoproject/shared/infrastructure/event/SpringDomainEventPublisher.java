package com.lingo.lingoproject.shared.infrastructure.event;

import com.lingo.lingoproject.shared.domain.event.DomainEvent;
import com.lingo.lingoproject.shared.domain.event.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 도메인 이벤트를 Spring의 이벤트 시스템으로 발행하는 어댑터.
 *
 * <h2>도메인 이벤트 시스템 구조</h2>
 * <pre>
 * [Service/UseCase]
 *   → DomainEventPublisher.publish(event)    ← 도메인 계층의 인터페이스 (인프라 미의존)
 *     → SpringDomainEventPublisher           ← 인프라 계층의 구현체 (Spring 의존)
 *       → ApplicationEventPublisher          ← Spring의 이벤트 발행 메커니즘
 *         → @EventListener / @TransactionalEventListener  ← 각 핸들러
 * </pre>
 *
 * <h2>인터페이스 분리 이유</h2>
 * <p>서비스/도메인 계층에서 {@link DomainEventPublisher} 인터페이스를 의존함으로써
 * Spring에 대한 직접 의존을 제거합니다. 테스트 시 Mock으로 교체하기 쉬워집니다.</p>
 *
 * <h2>현재 발행되는 이벤트 목록</h2>
 * <ul>
 *   <li>{@link com.lingo.lingoproject.matching.domain.event.MatchingRequestedEvent}: 매칭 요청 발생 시</li>
 *   <li>{@link com.lingo.lingoproject.matching.domain.event.MatchingAcceptedEvent}: 매칭 수락 시</li>
 *   <li>{@link com.lingo.lingoproject.report.domain.event.UserSuspendedEvent}: 유저 영구 정지 시</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class SpringDomainEventPublisher implements DomainEventPublisher {

  private final ApplicationEventPublisher applicationEventPublisher;

  /**
   * 도메인 이벤트를 Spring ApplicationEventPublisher를 통해 발행합니다.
   * {@code @TransactionalEventListener}를 사용하는 핸들러는
   * 트랜잭션 커밋 이후에 실행됩니다.
   *
   * @param event 발행할 도메인 이벤트
   */
  @Override
  public void publish(DomainEvent event) {
    applicationEventPublisher.publishEvent(event);
  }
}
