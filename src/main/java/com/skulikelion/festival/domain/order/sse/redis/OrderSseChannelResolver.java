/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.order.sse.redis;

import org.springframework.stereotype.Component;

import com.skulikelion.festival.domain.order.sse.OrderSseSubscribeType;

@Component
public class OrderSseChannelResolver {

  private static final String PREFIX = "sse:booth:";
  private static final String DELIMITER = ":";

  public String toChannel(Long boothId, OrderSseSubscribeType type) {
    return PREFIX + boothId + DELIMITER + type;
  }

  public OrderSseChannelInfo parse(String channel) {
    String remainder = channel.substring(PREFIX.length());
    String[] parts = remainder.split(DELIMITER);
    return new OrderSseChannelInfo(
        Long.parseLong(parts[0]), OrderSseSubscribeType.valueOf(parts[1]));
  }

  public boolean supports(String channel) {
    return channel.startsWith(PREFIX);
  }
}
