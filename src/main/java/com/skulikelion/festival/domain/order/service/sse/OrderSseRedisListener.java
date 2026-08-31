package com.skulikelion.festival.domain.order.service.sse;

import com.skulikelion.festival.domain.order.service.sse.store.LocalOrderSseEmitterStore;
import com.skulikelion.festival.domain.order.service.sse.store.OrderSseEmitterRegistry;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderSseRedisListener implements MessageListener {

    private final LocalOrderSseEmitterStore store;
    private final OrderSseChannelResolver channelResolver;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte @Nullable [] pattern) {
        String channel = new String(message.getChannel());
        OrderSseChannelInfo info = channelResolver.parse(channel);

        SseEventMessage eventMessage = objectMapper.readValue(message.getBody(), SseEventMessage.class);

        List<SseEmitter> targets = store.findByBoothIdAndSubscribeType(info.boothId(), info.subscribeType());
        for (SseEmitter emitter : targets) {
            try {
                emitter.send(SseEmitter.event().name(eventMessage.eventName()).data(eventMessage.payload()));
            } catch (IOException e) {
                store.remove(info.boothId(), info.subscribeType(), emitter);
            }
        }
    }
}
