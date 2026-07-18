package com.skulikelion.festival.domain.order.scheduler;

import com.skulikelion.festival.domain.order.service.idempotency.DbOrderIdempotencyKeyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DbOrderIdempotencyScheduler {

    private final DbOrderIdempotencyKeyManager idempotencyKeyManager;

    @Scheduled(cron = "0 0 1 * * *")
    public void cleanupIdempotencyKeys() {
        idempotencyKeyManager.deleteExpiredIdempotencyKeys(LocalDateTime.now().minusMinutes(10));
    }
}
