/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.util.idempotency.strategy;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.skulikelion.festival.global.exception.CustomException;

@Component
public class IdempotencyStrategyFactory {

  private final Map<IdempotencyType, IdempotencyStrategy> strategyMap;

  public IdempotencyStrategyFactory(List<IdempotencyStrategy> strategies) {
    this.strategyMap =
        strategies.stream().collect(Collectors.toMap(IdempotencyStrategy::getType, s -> s));
  }

  public IdempotencyStrategy getStrategy(IdempotencyType type) {
    return Optional.ofNullable(strategyMap.get(type))
        .orElseThrow(() -> new CustomException(IdempotencyErrorCode.UNSUPPORTED_TYPE_ERROR));
  }
}
