/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import com.skulikelion.festival.domain.booth.service.booth.BoothService;
import com.skulikelion.festival.domain.manager.service.ManagerService;
import com.skulikelion.festival.domain.order.sse.OrderSseNotifier;
import com.skulikelion.festival.domain.order.sse.OrderSseSubscriber;
import com.skulikelion.festival.domain.order.sse.local.LocalOrderSseNotifier;
import com.skulikelion.festival.domain.order.sse.local.LocalOrderSseSubscriber;
import com.skulikelion.festival.domain.order.sse.redis.*;
import com.skulikelion.festival.domain.order.sse.store.LocalOrderSseEmitterStore;
import com.skulikelion.festival.domain.order.sse.store.OrderSseEmitterFinder;
import com.skulikelion.festival.domain.order.sse.store.OrderSseEmitterRegistry;
import com.skulikelion.festival.global.config.property.OrderSseProperties;

import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableConfigurationProperties(OrderSseProperties.class)
public class OrderSseConfig {

  @Bean
  LocalOrderSseSubscriber localOrderSseSubscriber(
      OrderSseEmitterRegistry registry, BoothService boothService, ManagerService managerService) {
    return new LocalOrderSseSubscriber(boothService, managerService, registry);
  }

  @Bean
  @Primary
  @ConditionalOnProperty(name = "sse.strategy", havingValue = "local", matchIfMissing = true)
  public OrderSseSubscriber localOnlySubscriber(LocalOrderSseSubscriber subscriber) {
    return subscriber;
  }

  @Bean
  public LocalOrderSseNotifier localOrderSseNotifier(LocalOrderSseEmitterStore store) {
    return new LocalOrderSseNotifier(store);
  }

  @Bean
  @Primary
  @ConditionalOnProperty(name = "sse.strategy", havingValue = "local", matchIfMissing = true)
  public OrderSseNotifier localOnlyNotifier(LocalOrderSseNotifier sseNotifier) {
    return sseNotifier;
  }

  @Bean
  @Primary
  @ConditionalOnProperty(name = "sse.strategy", havingValue = "distributed")
  public OrderSseNotifier distributedNotifier(OrderSseEventPublisher publisher) {
    return new DistributedOrderSseNotifier(publisher);
  }

  @Bean
  @ConditionalOnProperty(name = "sse.strategy", havingValue = "distributed")
  public RedisOrderSseEventPublisher redisOrderSseEventPublisher(
      RedisTemplate<String, String> redisTemplate,
      OrderSseChannelResolver channelResolver,
      ObjectMapper objectMapper) {
    return new RedisOrderSseEventPublisher(redisTemplate, channelResolver, objectMapper);
  }

  @Bean
  @Primary
  @ConditionalOnProperty(name = "sse.redis.subscription-mode", havingValue = "dynamic")
  public OrderSseSubscriber dynamicOrderSseSubscriber(
      LocalOrderSseSubscriber localSubscriber,
      RedisMessageListenerContainer listenerContainer,
      OrderSseChannelResolver channelResolver,
      OrderSseEmitterFinder emitterFinder,
      OrderSseRedisListener listener,
      BoothService boothService) {
    return new RedisOrderSseSubscriber(
        localSubscriber, listenerContainer, channelResolver, emitterFinder, listener, boothService);
  }
}
