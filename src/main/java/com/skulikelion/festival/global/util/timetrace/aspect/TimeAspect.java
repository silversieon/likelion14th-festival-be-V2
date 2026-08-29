/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.util.timetrace.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.skulikelion.festival.global.util.timetrace.annotation.TimeTrace;

import lombok.extern.slf4j.Slf4j;

/**
 * 수행시간 관련 관심사를 해결하는 Advisor 클래스입니다. <br>
 * <br>
 *
 * @see TimeTrace
 * @since 2026.03.06
 * @author Keum Si Eon
 */
@Slf4j
@Aspect
@Component
public class TimeAspect {

  private final Environment env;

  public TimeAspect(Environment env) {
    this.env = env;
  }

  @Around("@annotation(timeTrace)")
  public Object doTimeLog(ProceedingJoinPoint joinPoint, TimeTrace timeTrace) throws Throwable {

    String[] activeProfiles = env.getActiveProfiles();
    if (!isProfileMatched(activeProfiles, timeTrace.env())) {
      return joinPoint.proceed();
    }

    String methodName = timeTrace.methodName();
    if (methodName.isEmpty()) {
      methodName = joinPoint.getSignature().getName();
    }

    long startTime = System.currentTimeMillis();
    Object result = joinPoint.proceed();
    long endTime = System.currentTimeMillis();
    long resultTime = endTime - startTime;
    log.info(
        "[TimeLog] {} - {}: {} ms",
        joinPoint.getSignature().getDeclaringType().getSimpleName(),
        methodName,
        resultTime);
    return result;
  }

  private boolean isProfileMatched(String[] activeProfiles, String[] timeTraceProfiles) {

    if (timeTraceProfiles.length == 0) {
      return true;
    }

    for (String activeProfile : activeProfiles) {
      for (String timeTraceProfile : timeTraceProfiles) {
        if (activeProfile.equals(timeTraceProfile)) {
          return true;
        }
      }
    }
    return false;
  }
}
