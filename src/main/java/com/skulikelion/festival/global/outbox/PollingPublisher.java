/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.outbox;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import com.skulikelion.festival.domain.order.event.OrderEventDispatcher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class PollingPublisher {

  private final OutboxRepository repository;
  private final OrderEventDispatcher orderEventDispatcher;

  @Scheduled(fixedDelay = 200)
  @Transactional
  public void publishPendingEvents() {
    List<Outbox> pendingEvents = repository.findPendingEvents();
    for (Outbox outbox : pendingEvents) {
      if (outbox.getAggregateType().equals(AggregateType.ORDER)) {
        log.info("Publishing pending events");
        orderEventDispatcher.handleOrderEvent(outbox.getPayload(), outbox.getEventType());
      }
      outbox.markAsPublished();
    }
  }
}
