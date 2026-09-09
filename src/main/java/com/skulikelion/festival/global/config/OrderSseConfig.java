/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

import com.skulikelion.festival.domain.booth.service.booth.BoothService;
import com.skulikelion.festival.domain.manager.service.ManagerService;
import com.skulikelion.festival.domain.order.sse.OrderSseNotifier;
import com.skulikelion.festival.domain.order.sse.OrderSseSubscriber;
import com.skulikelion.festival.domain.order.sse.local.LocalOrderSseNotifier;
import com.skulikelion.festival.domain.order.sse.local.LocalOrderSseSubscriber;
import com.skulikelion.festival.domain.order.sse.redis.*;
import com.skulikelion.festival.domain.order.sse.redis.pubsub.OrderPubSubEventPublisher;
import com.skulikelion.festival.domain.order.sse.redis.pubsub.OrderPubSubListener;
import com.skulikelion.festival.domain.order.sse.redis.pubsub.OrderPubSubSseSubscriber;
import com.skulikelion.festival.domain.order.sse.redis.stream.OrderStreamEventPublisher;
import com.skulikelion.festival.domain.order.sse.redis.stream.OrderStreamListener;
import com.skulikelion.festival.domain.order.sse.redis.stream.OrderStreamSseSubscriber;
import com.skulikelion.festival.domain.order.sse.store.LocalOrderSseEmitterStore;
import com.skulikelion.festival.domain.order.sse.store.OrderSseEmitterRegistry;
import com.skulikelion.festival.global.config.property.OrderSseProperties;

import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableConfigurationProperties(OrderSseProperties.class)
public class OrderSseConfig {

  @Bean
  @ConditionalOnProperty(name = "sse.strategy", havingValue = "local", matchIfMissing = true)
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
  @Primary
  @ConditionalOnProperty(name = "sse.strategy", havingValue = "distributed")
  @ConditionalOnProperty(name = "sse.redis.mode", havingValue = "pubsub")
  public OrderSseEventPublisher redisOrderPubSubEventPublisher(
      RedisTemplate<String, String> redisTemplate,
      OrderSseChannelResolver channelResolver,
      ObjectMapper objectMapper) {
    return new OrderPubSubEventPublisher(redisTemplate, channelResolver, objectMapper);
  }

  @Bean
  @ConditionalOnProperty(name = "sse.redis.mode", havingValue = "pubsub")
  @ConditionalOnProperty(name = "sse.redis.subscription-mode", havingValue = "dynamic")
  public OrderPubSubListener redisOrderPubSubListener(
      OrderSseChannelResolver channelResolver,
      ObjectMapper objectMapper,
      OrderSseDispatcher dispatcher) {
    return new OrderPubSubListener(channelResolver, objectMapper, dispatcher);
  }

  @Bean
  @Primary
  @ConditionalOnProperty(name = "sse.redis.mode", havingValue = "pubsub")
  @ConditionalOnProperty(name = "sse.redis.subscription-mode", havingValue = "dynamic")
  public OrderSseSubscriber dynamicOrderSseSubscriber(
      RedisMessageListenerContainer listenerContainer,
      OrderSseChannelResolver channelResolver,
      OrderSseEmitterRegistry emitterRegistry,
      OrderPubSubListener listener,
      BoothService boothService) {
    return new OrderPubSubSseSubscriber(
        listenerContainer, channelResolver, emitterRegistry, listener, boothService);
  }

  @Bean
  @ConditionalOnProperty(name = "sse.strategy", havingValue = "distributed")
  @ConditionalOnProperty(name = "sse.redis.mode", havingValue = "stream")
  public OrderStreamListener orderStreamListener(
      RedisTemplate<String, String> redisTemplate,
      ObjectMapper objectMapper,
      OrderSseChannelResolver channelResolver,
      String instanceId,
      OrderSseDispatcher dispatcher) {
    return new OrderStreamListener(
        redisTemplate, objectMapper, channelResolver, instanceId, dispatcher);
  }

  @Bean
  @Primary
  @ConditionalOnProperty(name = "sse.strategy", havingValue = "distributed")
  @ConditionalOnProperty(name = "sse.redis.mode", havingValue = "stream")
  public OrderSseSubscriber streamOrderSseSubscriber(
      RedisTemplate<String, String> redisTemplate,
      StreamMessageListenerContainer<String, MapRecord<String, String, String>> listenerContainer,
      OrderSseChannelResolver channelResolver,
      OrderSseEmitterRegistry emitterRegistry,
      OrderStreamListener listener,
      String instanceId,
      BoothService boothService) {
    return new OrderStreamSseSubscriber(
        redisTemplate,
        listenerContainer,
        channelResolver,
        emitterRegistry,
        boothService,
        instanceId,
        listener);
  }

  @Bean
  @Primary
  @ConditionalOnProperty(name = "sse.strategy", havingValue = "distributed")
  @ConditionalOnProperty(name = "sse.redis.mode", havingValue = "stream")
  public OrderSseEventPublisher streamOrderSseEventPublisher(
      RedisTemplate<String, String> redisTemplate,
      OrderSseChannelResolver channelResolver,
      ObjectMapper objectMapper) {
    return new OrderStreamEventPublisher(redisTemplate, channelResolver, objectMapper);
  }
}
