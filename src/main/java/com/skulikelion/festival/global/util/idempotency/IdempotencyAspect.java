/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.util.idempotency.aspect;

import com.skulikelion.festival.global.util.idempotency.strategy.IdempotencyStrategyFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import com.skulikelion.festival.global.util.idempotency.IdempotencyStrategy;
import com.skulikelion.festival.global.util.idempotency.annotation.Idempotent;
import com.skulikelion.festival.global.util.idempotency.strategy.db.DbIdempotencyStrategy;
import com.skulikelion.festival.global.util.idempotency.strategy.fallback.FallbackIdempotencyStrategy;
import com.skulikelion.festival.global.util.idempotency.strategy.redis.RedisIdempotencyStrategy;
import com.skulikelion.festival.global.util.idempotency.strategy.writethrough.WriteThroughIdempotencyStrategy;

import lombok.RequiredArgsConstructor;

@Aspect
@Component
@Order(0)
@RequiredArgsConstructor
public class IdempotencyAspect {

  private final IdempotencyStrategyFactory strategyFactory;

  @Around("@annotation(idempotent)")
  public Object doIdempotency(ProceedingJoinPoint joinPoint, Idempotent idempotent) {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();

    StandardEvaluationContext context = getMethodArgsContext(signature, joinPoint);

    String idempotencyKey =
        new SpelExpressionParser()
            .parseExpression(idempotent.idempotencyKey())
            .getValue(context, String.class);

    Class<?> returnType = signature.getReturnType();

    IdempotencyStrategy strategy = strategyFactory.getStrategy(idempotent.strategy());

    return strategy.executeIdempotent(
        idempotencyKey,
        () -> {
          try {
            return joinPoint.proceed();
          } catch (Throwable e) {
            throw new RuntimeException(e);
          }
        },
        (Class) returnType);
  }

  private StandardEvaluationContext getMethodArgsContext(
      MethodSignature signature, ProceedingJoinPoint joinPoint) {
    String[] paramNames = signature.getParameterNames();
    Object[] args = joinPoint.getArgs();

    StandardEvaluationContext context = new StandardEvaluationContext();
    for (int i = 0; i < paramNames.length; i++) {
      context.setVariable(paramNames[i], args[i]);
    }
    return context;
  }
}
