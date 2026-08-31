package com.skulikelion.festival.domain.order.service.sse;

import com.skulikelion.festival.domain.order.enums.SseSubscribeType;
import org.springframework.stereotype.Component;

@Component
public class OrderSseChannelResolver {

    private static final String PREFIX = "sse:booth:";
    private static final String DELIMITER = ":";

    public String toChannel(Long boothId, SseSubscribeType type) {
        return PREFIX + boothId + DELIMITER + type;
    }

    public OrderSseChannelInfo parse(String channel) {
        String remainder = channel.substring(PREFIX.length());
        String[] parts = remainder.split(DELIMITER);
        return new OrderSseChannelInfo(Long.parseLong(parts[0]), SseSubscribeType.valueOf(parts[1]));
    }

    public boolean supports(String channel) {
        return channel.startsWith(PREFIX);
    }
}
