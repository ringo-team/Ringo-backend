package com.lingo.lingoproject.common.utils.log;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LoggingAspect {
  @Around("execution(* com.lingo.lingoproject..*Controller.*(..))")
  public Object loggingController(ProceedingJoinPoint joinPoint) throws Throwable {

    if (((MethodSignature) joinPoint.getSignature()).getMethod().isAnnotationPresent(Scheduled.class)) {
      Object result = joinPoint.proceed();
      return result;
    }

    log.info("request:: class={} method={}",
        joinPoint.getSignature().getDeclaringType(),
        joinPoint.getSignature().getName());

    long startTime = System.currentTimeMillis();
    Object result = null;

    result = joinPoint.proceed();

    long endTime = System.currentTimeMillis();

    log.info("method={} response={}, result={}, elapsedMs = {}, status={}",
        joinPoint.getSignature().getName(),
        joinPoint.getSignature().getName(),
        result != null ? result.getClass().getSimpleName() : null,
        endTime - startTime, "SUCCESS");

    return result;
  }
}
