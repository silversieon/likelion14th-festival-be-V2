/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.sse.redis.stream;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.PostConstruct;

import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.skulikelion.festival.domain.booth.service.booth.BoothService;
import com.skulikelion.festival.domain.order.sse.OrderSseSubscribeType;
import com.skulikelion.festival.domain.order.sse.OrderSseSubscriber;
import com.skulikelion.festival.domain.order.sse.redis.OrderSseChannelResolver;
import com.skulikelion.festival.domain.order.sse.store.OrderSseEmitterRegistry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class OrderStreamSseSubscriber implements OrderSseSubscriber {

  private final RedisTemplate<String, String> redisTemplate;
  private final StreamMessageListenerContainer<String, MapRecord<String, String, String>>
      listenerContainer;
  private final OrderSseChannelResolver channelResolver;
  private final OrderSseEmitterRegistry registry;
  private final BoothService boothService;
  private final String instanceId;
  private final OrderStreamListener orderStreamListener;

  private final Map<String, Subscription> activeSubscriptions = new ConcurrentHashMap<>();

  private String groupName;

  @PostConstruct
  public void init() {
    this.groupName = "sse-consumer-group-" + instanceId;
  }

  @Override
  public SseEmitter subscribeOrderStatus(
      String departmentName, OrderSseSubscribeType subscribeType) {
    Long boothId = boothService.getRequiredBooth(departmentName).getId();
    SseEmitter emitter = new SseEmitter(60 * 60 * 1000L);

    boolean isFirst = registry.registerAndCheckFirst(boothId, subscribeType, emitter);
    String streamKey = channelResolver.toChannel(boothId, subscribeType);
    if (isFirst) {
      subscribe(streamKey);
    }

    registerUnsubscribeCallback(emitter, boothId, subscribeType, streamKey);
    connectCheck(emitter, boothId, subscribeType);
    return emitter;
  }

  private void subscribe(String streamKey) {
    createGroupIfNotExists(streamKey, groupName);
    Subscription subscription =
        listenerContainer.receive(
            Consumer.from(groupName, "consumer-" + instanceId),
            StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
            orderStreamListener);
    activeSubscriptions.put(streamKey, subscription);
  }

  private void registerUnsubscribeCallback(
      SseEmitter emitter, Long boothId, OrderSseSubscribeType subscribeType, String streamKey) {

    emitter.onCompletion(
        () -> {
          log.debug("[OrderSseService] SSE 연결 종료 - 부스 식별자: {}, 구독 타입: {}", boothId, subscribeType);
          boolean isLast = registry.removeAndCheckLast(boothId, subscribeType, emitter);
          if (isLast) {
            Subscription subscription = activeSubscriptions.remove(streamKey);
            if (subscription != null) {
              listenerContainer.remove(subscription);
            }
            cleanupGroup(streamKey, groupName);
          }
        });
    emitter.onTimeout(
        () -> {
          log.debug("[OrderSseService] SSE 타임아웃 - 부스 식별자: {}, 구독 타입: {}", boothId, subscribeType);
          boolean isLast = registry.removeAndCheckLast(boothId, subscribeType, emitter);
          if (isLast) {
            Subscription subscription = activeSubscriptions.remove(streamKey);
            if (subscription != null) {
              listenerContainer.remove(subscription);
            }
            cleanupGroup(streamKey, groupName);
          }
          emitter.complete();
        });
    emitter.onError(
        e -> {
          log.warn("[OrderSseService] SSE 에러 - 부스 식별자: {}, 구독 타입: {}", boothId, subscribeType);
          boolean isLast = registry.removeAndCheckLast(boothId, subscribeType, emitter);
          if (isLast) {
            Subscription subscription = activeSubscriptions.remove(streamKey);
            if (subscription != null) {
              listenerContainer.remove(subscription);
            }
            cleanupGroup(streamKey, groupName);
          }
          emitter.complete();
        });
  }

  private void createGroupIfNotExists(String streamKey, String groupName) {
    try {
      redisTemplate.opsForStream().createGroup(streamKey, ReadOffset.from("$"), groupName);
    } catch (RedisSystemException e) {
      if (!e.getMessage().contains("BUSYGROUP")) throw e;
    }
  }

  private void cleanupGroup(String streamKey, String groupName) {
    try {
      redisTemplate.opsForStream().destroyGroup(streamKey, groupName);
    } catch (Exception ignored) {
      // 이미 없거나 스트림이 사라진 경우 무시
    }
  }

  private void connectCheck(SseEmitter emitter, Long boothId, OrderSseSubscribeType subscribeType) {
    try {
      emitter.send(SseEmitter.event().name("connect").data("connected order subscribe"));
    } catch (IOException e) {
      registry.remove(boothId, subscribeType, emitter);
    }
  }
}
