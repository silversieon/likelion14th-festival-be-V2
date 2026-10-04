/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.sse.redis.stream;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import jakarta.annotation.PostConstruct;

import org.springframework.data.domain.Range;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.skulikelion.festival.domain.booth.service.booth.BoothService;
import com.skulikelion.festival.domain.order.sse.OrderSseSubscribeType;
import com.skulikelion.festival.domain.order.sse.OrderSseSubscriber;
import com.skulikelion.festival.domain.order.sse.redis.OrderSseChannelResolver;
import com.skulikelion.festival.domain.order.sse.store.OrderSseEmitterRegistry;
import com.skulikelion.festival.global.security.AuthPrincipal;

import io.lettuce.core.RedisBusyException;
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
  private final Map<String, ReentrantLock> streamLocks = new ConcurrentHashMap<>();

  private String groupName;
  private String consumerName;

  @PostConstruct
  public void init() {
    this.groupName = "sse-consumer-group-" + instanceId;
    this.consumerName = "consumer-" + instanceId;
  }

  @Override
  public SseEmitter subscribeOrderStatus(
      AuthPrincipal principal, OrderSseSubscribeType subscribeType) {
    Long boothId = boothService.getRequiredBooth(principal).getId();
    String streamKey = channelResolver.toChannel(boothId, subscribeType);

    SseEmitter emitter = new SseEmitter(60 * 60 * 1000L);
    emitter.onCompletion(() -> unsubscribe(emitter, boothId, subscribeType, streamKey));
    emitter.onTimeout(emitter::complete);
    emitter.onError(e -> emitter.complete());

    ReentrantLock lock = lockFor(streamKey);
    lock.lock();
    try {
      boolean isFirst = registry.registerAndCheckFirst(boothId, subscribeType, emitter);
      if (isFirst) {
        try {
          subscribe(streamKey);
        } catch (RuntimeException e) {
          registry.remove(boothId, subscribeType, emitter);
          throw e;
        }
      }
    } finally {
      lock.unlock();
    }

    connectCheck(emitter, boothId, subscribeType);
    return emitter;
  }

  private void subscribe(String streamKey) {
    createGroupIfNotExists(streamKey, groupName);
    try {
      discardPoisonMessages(streamKey);
      drainPending(streamKey);
    } catch (Exception e) {
      log.warn("[OrderStream] PEL 정리 실패, 구독은 계속 진행: stream={}", streamKey, e);
    }

    Subscription subscription =
        listenerContainer.receive(
            Consumer.from(groupName, consumerName),
            StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
            orderStreamListener);
    activeSubscriptions.put(streamKey, subscription);
  }

  private void unsubscribe(
      SseEmitter emitter, Long boothId, OrderSseSubscribeType subscribeType, String streamKey) {
    ReentrantLock lock = lockFor(streamKey);
    try {
      boolean isLast = registry.removeAndCheckLast(boothId, subscribeType, emitter);
      if (!isLast) return;

      Subscription subscription = activeSubscriptions.remove(streamKey);
      if (subscription != null) {
        listenerContainer.remove(subscription);
      }
    } finally {
      lock.unlock();
    }
  }

  private void createGroupIfNotExists(String streamKey, String groupName) {
    try {
      redisTemplate.opsForStream().createGroup(streamKey, ReadOffset.from("$"), groupName);
    } catch (RedisSystemException e) {
      // 컨슈머 그룹이 고정이기 때문에 RedisBusyException은 정상 예외 (이 외에만 던짐)
      if (!(e.getCause() instanceof RedisBusyException)) throw e;
    }
  }

  private void discardPoisonMessages(String streamKey) {
    PendingMessages pending =
        redisTemplate
            .opsForStream()
            .pending(streamKey, Consumer.from(groupName, consumerName), Range.unbounded(), 100);

    for (PendingMessage pm : pending) {
      if (pm.getTotalDeliveryCount() >= 5) {
        log.error("[PEL] 재시도 한도 초과로 폐기: stream={}, id={}", streamKey, pm.getIdAsString());
        redisTemplate.opsForStream().acknowledge(streamKey, groupName, pm.getId());
      }
    }
  }

  private void drainPending(String streamKey) {
    StreamOperations<String, String, String> ops = redisTemplate.opsForStream();
    String lastId = "0";

    while (true) {
      List<MapRecord<String, String, String>> records =
          ops.read(
              Consumer.from(groupName, consumerName),
              StreamReadOptions.empty().count(100),
              StreamOffset.create(streamKey, ReadOffset.from(lastId)));

      if (records == null || records.isEmpty()) return;

      for (MapRecord<String, String, String> record : records) {
        orderStreamListener.onMessage(record);
        lastId = record.getId().getValue();
      }
    }
  }

  private void connectCheck(SseEmitter emitter, Long boothId, OrderSseSubscribeType subscribeType) {
    try {
      emitter.send(SseEmitter.event().name("connect").data("connected order subscribe"));
    } catch (IOException e) {
      registry.remove(boothId, subscribeType, emitter);
    }
  }

  private ReentrantLock lockFor(String streamKey) {
    return streamLocks.computeIfAbsent(streamKey, k -> new ReentrantLock());
  }
}
