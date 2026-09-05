/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.skulikelion.festival.domain.order.event.OrderEventDispatcher;
import com.skulikelion.festival.domain.order.event.listener.OrderDirectEventListener;
import com.skulikelion.festival.domain.order.event.listener.OrderEventListener;
import com.skulikelion.festival.domain.order.event.listener.OrderOutboxEventListener;
import com.skulikelion.festival.domain.order.sse.OrderSseNotifier;
import com.skulikelion.festival.global.outbox.OutboxRepository;
import com.skulikelion.festival.global.outbox.PollingPublisher;

import tools.jackson.databind.ObjectMapper;

@Configuration
public class OrderEventConfig {

  @Bean
  @ConditionalOnProperty(
      name = "order.event.pattern",
      havingValue = "direct",
      matchIfMissing = true)
  public OrderEventListener orderDirectEventListener(
      OrderSseNotifier notifier, ObjectMapper objectMapper) {
    return new OrderDirectEventListener(notifier, objectMapper);
  }

  @Bean
  @ConditionalOnProperty(name = "order.event.pattern", havingValue = "outbox")
  public OrderEventListener orderOutboxEventListener(
      OutboxRepository outboxRepository, ObjectMapper objectMapper) {
    return new OrderOutboxEventListener(outboxRepository, objectMapper);
  }

  @Bean
  @ConditionalOnProperty(name = "order.event.pattern", havingValue = "outbox")
  public PollingPublisher pollingPublisher(
      OutboxRepository repository, OrderEventDispatcher orderEventDispatcher) {
    return new PollingPublisher(repository, orderEventDispatcher);
  }
}
