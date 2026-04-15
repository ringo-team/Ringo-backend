package com.lingo.lingoproject.shared.utils.log;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 컨트롤러 요청/응답 자동 로깅 AOP Aspect.
 *
 * <h2>역할</h2>
 * <p>모든 컨트롤러 메서드 호출 전후에 자동으로 로그를 남깁니다.
 * 개별 컨트롤러에서 중복으로 요청/응답 로그를 작성할 필요가 없습니다.</p>
 *
 * <h2>대상 범위</h2>
 * <p>포인트컷: {@code execution(* com.lingo.lingoproject..*Controller.*(..))}</p>
 * <ul>
 *   <li>패키지 하위의 모든 클래스명이 "Controller"로 끝나는 클래스의 모든 메서드</li>
 *   <li>WebSocket 컨트롤러, REST 컨트롤러 모두 포함</li>
 * </ul>
 *
 * <h2>출력 예시</h2>
 * <pre>
 * [요청] step=컨트롤러_요청, class=UserController, method=getUserInfo
 * ... (컨트롤러 메서드 실행) ...
 * [응답] step=컨트롤러_응답, method=getUserInfo, result=ResponseEntity, elapsedMs=42, status=SUCCESS
 * </pre>
 *
 * <h2>@Scheduled 제외</h2>
 * <p>{@link Scheduled} 어노테이션이 달린 메서드는 스케줄러가 주기적으로 호출하므로
 * 매번 로그가 찍히면 노이즈가 심합니다. 이 경우 로깅 없이 그대로 실행합니다.</p>
 *
 * <h2>예외 처리</h2>
 * <p>이 Aspect는 예외를 잡지 않습니다. 컨트롤러에서 예외가 발생하면
 * {@link com.lingo.lingoproject.shared.exception.RingoExceptionController}(ExceptionHandler)가
 * 처리합니다. 예외 로그는 해당 서비스/필터에서 별도로 남깁니다.</p>
 */
@Aspect
@Component
@Slf4j
public class LoggingAspect {

  /**
   * 컨트롤러 메서드를 감싸는 Around Advice.
   * 메서드 실행 전후에 로그를 남기고 실행 시간을 측정합니다.
   *
   * @param joinPoint AOP가 가로챈 메서드 실행 정보
   * @return 원본 메서드의 반환값
   * @throws Throwable 컨트롤러 메서드에서 발생한 예외를 그대로 전파
   */
  @Around("execution(* com.lingo.lingoproject..*Controller.*(..))")
  public Object loggingController(ProceedingJoinPoint joinPoint) throws Throwable {

    // @Scheduled 메서드는 로깅 없이 바로 실행
    if (((MethodSignature) joinPoint.getSignature()).getMethod().isAnnotationPresent(Scheduled.class)) {
      Object result = joinPoint.proceed();
      return result;
    }

    log.info("step=컨트롤러_요청, class={}, method={}",
        joinPoint.getSignature().getDeclaringType().getSimpleName(),
        joinPoint.getSignature().getName());

    long startTime = System.currentTimeMillis();
    Object result = null;

    // 실제 컨트롤러 메서드 실행
    result = joinPoint.proceed();

    long endTime = System.currentTimeMillis();

    log.info("step=컨트롤러_응답, method={}, result={}, elapsedMs={}, status=SUCCESS",
        joinPoint.getSignature().getName(),
        result != null ? result.getClass().getSimpleName() : null,
        endTime - startTime);

    return result;
  }
}
