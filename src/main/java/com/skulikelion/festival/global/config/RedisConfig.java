/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.config;

import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

import com.skulikelion.festival.domain.order.sse.redis.pubsub.OrderPubSubListener;

@Configuration
public class RedisConfig {

  @Bean
  public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
    redisTemplate.setConnectionFactory(connectionFactory);
    StringRedisSerializer serializer = new StringRedisSerializer();
    redisTemplate.setKeySerializer(serializer);
    redisTemplate.setValueSerializer(serializer);
    redisTemplate.setHashKeySerializer(serializer);
    redisTemplate.setHashValueSerializer(serializer);
    return redisTemplate;
  }

  @Bean
  @Primary
  @ConditionalOnProperty(name = "sse.strategy", havingValue = "distributed")
  @ConditionalOnProperty(name = "sse.redis.mode", havingValue = "pubsub", matchIfMissing = true)
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
  @ConditionalOnProperty(name = "sse.redis.mode", havingValue = "pubsub", matchIfMissing = true)
  @ConditionalOnProperty(
      name = "sse.redis.subscription-mode",
      havingValue = "pattern",
      matchIfMissing = true)
  public RedisMessageListenerContainer patternMessageListenerContainer(
      RedisConnectionFactory connectionFactory, OrderPubSubListener listener) {
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.addMessageListener(listener, new PatternTopic("sse:booth:*"));
    return container;
  }

  @Bean
  @Primary
  @ConditionalOnProperty(name = "sse.strategy", havingValue = "distributed")
  @ConditionalOnProperty(name = "sse.redis.mode", havingValue = "stream")
  public StreamMessageListenerContainer<String, MapRecord<String, String, String>>
      streamMessageListenerContainer(RedisConnectionFactory connectionFactory) {

    StreamMessageListenerContainer.StreamMessageListenerContainerOptions<
            String, MapRecord<String, String, String>>
        options =
            StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                .pollTimeout(Duration.ofMillis(500))
                .build();

    StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
        StreamMessageListenerContainer.create(connectionFactory, options);

    container.start();
    return container;
  }
}
