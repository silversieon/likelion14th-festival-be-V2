package com.skulikelion.festival.global.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PollingPublisher {

    private final OutboxRepository repository;

    @Scheduled(fixedDelay = 200)
    public void publishPendingEvents() {
        
    }
}
