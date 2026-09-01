/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import com.skulikelion.festival.domain.order.sse.redis.OrderSseRedisListener;

@Configuration
public class RedisPubSubConfig {

  @Bean
  @Primary
  @ConditionalOnProperty(name = "sse.strategy", havingValue = "distributed")
  @ConditionalOnProperty(name = "sse.redis.subscription-mode", havingValue = "dynamic")
  public RedisMessageListenerContainer redisMessageListenerContainer(
      RedisConnectionFactory connectionFactory) {
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    return container;
  }

  @Bean
  @Primary
  @ConditionalOnProperty(name = "sse.strategy", havingValue = "distributed")
  @ConditionalOnProperty(
      name = "sse.redis.subscription-mode",
      havingValue = "pattern",
      matchIfMissing = true)
  public RedisMessageListenerContainer patternMessageListenerContainer(
      RedisConnectionFactory connectionFactory, OrderSseRedisListener listener) {
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.addMessageListener(listener, new PatternTopic("sse:booth:*"));
    return container;
  }
}
